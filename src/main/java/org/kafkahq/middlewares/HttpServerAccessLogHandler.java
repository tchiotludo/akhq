package org.kafkahq.middlewares;

import com.google.common.net.HttpHeaders;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import javax.inject.Singleton;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * @author croudet
 */
@Sharable
@Singleton
@Requires(property = "kafkahq.server.access-log.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
public class HttpServerAccessLogHandler extends ChannelDuplexHandler {
    private final Logger accessLogger;
    private static String logFormat;
    private static final AttributeKey<AccessLog> LOG_HANDLER_CONTEXT = AttributeKey.valueOf("logHandlerContext");
    private static final String MISSING = "-";

    public HttpServerAccessLogHandler(@Value("${kafkahq.server.access-log.name}") String name,
                                      @Value("${kafkahq.server.access-log.format}") String format) {
        this(LoggerFactory.getLogger(name), format);
    }

    private HttpServerAccessLogHandler(Logger accessLogger, String format) {
        super();
        this.accessLogger = accessLogger;
        logFormat = format;
    }

    private static String inetAddress(SocketChannel channel, HttpRequest request) {
        // maybe this request was proxied or load balanced. Try and get the real originating IP
        final String proxyChain = request.headers().get(HttpHeaders.X_FORWARDED_FOR, null);

        if (proxyChain != null) {
            // can contain multiple IPs for proxy chains. the first ip is our
            // client.
            final int firstComma = proxyChain.indexOf(',');
            if (firstComma != -1) {
                return proxyChain.substring(0, firstComma);
            } else {
                return proxyChain;
            }
        } else {
            return channel.remoteAddress().getHostString();
        }
    }

    private static AccessLog accessLog(SocketChannel channel) {
        final Attribute<AccessLog> attr = channel.attr(LOG_HANDLER_CONTEXT);

        AccessLog accessLog = attr.get();
        if (accessLog == null) {
            accessLog = new AccessLog(logFormat);
            attr.set(accessLog);
        } else {
            accessLog.reset();
        }

        return accessLog;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (accessLogger.isInfoEnabled() && msg instanceof HttpRequest) {
            final SocketChannel channel = (SocketChannel) ctx.channel();
            AccessLog accessLog = accessLog(channel);
            final HttpRequest request = (HttpRequest) msg;
            accessLog.startTime = System.nanoTime();
            accessLog.inetAddress = inetAddress(channel, request);
            accessLog.port = channel.localAddress().getPort();
            accessLog.method = request.method().name();
            accessLog.uri = request.uri();
            accessLog.protocol = request.protocolVersion().text();
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // modify message on way out to add headers if needed
        if (accessLogger.isInfoEnabled()) {
            processWriteEvent(ctx, msg, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    private void logAtLast(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, AccessLog accessLog) {
        ctx.write(msg, promise).addListener(future -> {
            if (future.isSuccess()) {
                accessLog.logAccess(accessLogger);
            }
        });
    }

    private void processWriteEvent(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        final AccessLog accessLog = ctx.channel().attr(LOG_HANDLER_CONTEXT).get();
        if (accessLog != null && accessLog.method != null) {
            if (msg instanceof HttpResponse) {
                final HttpResponse response = (HttpResponse) msg;
                final HttpResponseStatus status = response.status();

                if (status.equals(HttpResponseStatus.CONTINUE)) {
                    ctx.write(msg, promise);
                    return;
                }
                final boolean chunked = HttpUtil.isTransferEncodingChunked(response);
                accessLog.chunked = chunked;
                accessLog.status = status.code();
                if (!chunked) {
                    accessLog.contentLength = HttpUtil.getContentLength(response, -1L);
                }
            }
            if (msg instanceof LastHttpContent) {
                accessLog.increaseContentLength(((LastHttpContent) msg).content().readableBytes());
                logAtLast(ctx, msg, promise, accessLog);
                return;
            } else if (msg instanceof ByteBuf) {
                accessLog.increaseContentLength(((ByteBuf) msg).readableBytes());
            } else if (msg instanceof ByteBufHolder) {
                accessLog.increaseContentLength(((ByteBufHolder) msg).content().readableBytes());
            }
        }

        super.write(ctx, msg, promise);
    }

    private static class AccessLog {
        private String logFormat;
        private String inetAddress;
        private String method;
        private String uri;
        private String protocol;
        private int port;
        private boolean chunked;
        private int status;
        private long startTime;
        private long contentLength;
        private String zonedDateTime;

        AccessLog(String logFormat) {
            this.logFormat = logFormat;
            this.zonedDateTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }

        private void reset() {
            this.zonedDateTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            inetAddress = null;
            method = null;
            uri = null;
            protocol = null;
            port = -1;
            status = -1;
            startTime = 0L;
            contentLength = 0L;
            chunked = false;
        }

        void increaseContentLength(long contentLength) {
            if (chunked) {
                this.contentLength += contentLength;
            }
        }

        @SuppressWarnings("boxing")
        void logAccess(Logger accessLogger) {
            if (accessLogger.isInfoEnabled()) {
                final long timeElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                String message = MessageFormatter.arrayFormat(logFormat, new Object[]{
                    zonedDateTime,
                    timeElapsed,
                    method,
                    uri,
                    protocol,
                    status,
                    inetAddress,
                    contentLength > -1L ? contentLength : MISSING,
                    port
                }).getMessage();


                if (status >= 400) {
                    accessLogger.warn(message);
                } else {
                    accessLogger.info(message);
                }
            }
        }
    }

}