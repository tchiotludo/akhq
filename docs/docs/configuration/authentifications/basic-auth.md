# Basic Auth
* `akhq.security.basic-auth`: List user & password with affected roles
  * `- username: actual-username`: Login of the current user as (maybe anything email, login, ...)
    * `password`: Password in sha256 (default) or bcrypt. The password can be converted
      * For default SHA256, with command `echo -n "password" | sha256sum` or Ansible filter <code v-pre>{{ 'password' | hash('sha256') }}</code>
      * For BCrypt, with Ansible filter <code v-pre>{{ 'password' | password_hash('blowfish') }}</code>
    * `passwordHash`: Password hashing algorithm, either `SHA256` or `BCRYPT`
    * `groups`: Groups for current user

Configure basic-auth connection in AKHQ

```yaml
micronaut:
  security:
    enabled: true
akhq.security:
  basic-auth:
    - username: admin
      password: "$2a$<hashed password>"
      passwordHash: BCRYPT
      groups:
      - admin
    - username: reader
      password: "<SHA-256 hashed password>"
      groups:
      - reader
```
