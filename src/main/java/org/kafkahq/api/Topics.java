package org.kafkahq.api;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.kafkahq.configs.Role;
import org.kafkahq.models.Topic;
import org.kafkahq.repositories.TopicRepository;
import org.kafkahq.utils.CompletablePaged;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Secured(Role.ROLE_TOPIC_READ)
@Controller("${kafkahq.server.base-path:}/api/{cluster}/topic")
public class Topics {

	private TopicRepository topicRepository;
	@Value("${kafkahq.topic.default-view}")
	private String defaultView;
	@Value("${kafkahq.topic.page-size:25}")
	private Integer pageSize;

	@Inject
	public Topics(TopicRepository topicRepository)
	{
		this.topicRepository = topicRepository;
	}

	@Get
	public HttpResponse<?> list(
			HttpRequest request, String cluster,
			Optional<String> search,
			Optional<TopicRepository.TopicListView> show,
			Optional<Integer> page
	) throws ExecutionException, InterruptedException {

		List<CompletableFuture<Topic>> list = this.topicRepository.list(
				cluster,
				show.orElse(TopicRepository.TopicListView.valueOf(defaultView)),
				search
		);

		URIBuilder uri = URIBuilder.fromURI(request.getUri());
		CompletablePaged<Topic> paged = new CompletablePaged<>(
				list,
				this.pageSize,
				uri,
				page.orElse(1)
		);

		return HttpResponse.ok().body(paged.complete());
	}
}
