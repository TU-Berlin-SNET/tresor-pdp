# Tresor PDP

The Tresor PDP is an Policy Decision Point (PDP) created in the context of the [tresor project](http://www.cloud-tresor.de) which supports XACML2, XACML3 and geoXACML for both though non-standard in the case of XACML3.

[Balana](https://www.github.com/wso2/balana) is used as a base for decision processing, extended by geoXACML components from [geotools](https://github.com/geotools/geotools) community and specific policystore implementations (among other extensions) as well as a contexthandler providing a RESTful API.

## Configuration
Configuration for the tresor-pdp is provided through yaml or (albeit very limited) through the command line.

The file application.yml in modules/contexthandler/src/main/resources/ is used (and packaged) by default. It is possible to either edit this file or provide the path to another via the command line.

Example application.yml:   
~~~yaml
##
## Usage:
##
## pdp:                                     # beginning of configuration
##   locationpips:                          # (optional) configuration for locationpips
##     -
##       url: <url>                         # url to locationpip
##       authentication: <authentication>   # (optional) http-basic authentication header
##   stationpips:                           # (optional) configuration of stationpips
##     -
##       url: <url>                         # url to stationpip
##   policystore:                           # (optional) policystore configuration
##     type: file                           # supported are file and redis, default: file
##     path: <path_to_directory>            # (optional) path to policies dir, default: same as jar
##
## or use redis policystore (only one policystore possible)
##
##   policystore:
##     type: redis
##     host: <host>                         # (optional) host of redisDB, default: localhost
##     port: <port>                         # (optional) port of redisDB, default: 6379
##     timeout: <timeout>                   # (optional) timeout in ms for redisDB queries, default: 2000
##     password: <password>                 # (optional) password for redisDB

pdp:
  locationpips:
    -
      url: localhost
      authentication: Basic dGhpc19pczpub3R0aGVwYXNzd29yZA==
  policystore:
    type: redis
    host: localhost
    port: 9999
    timeout: 3000
~~~

Arguments for the command line:   
~~~bash
    --server.port=PORT                  binds server instance to port  
    --spring.config.location=PATH       location of .yml config file to load  
    --policystore.path=PATH             stores policies in given directory path, overriding any other settings  
~~~

## Usage
Package the whole project with mvn package. 
Optionally move/deploy the created jar file modules/pdp-contexthandler/target/pdp-contexthandler.jar somewhere. 
Run it with java -jar. That's it.

It is __strongly recommended__ to set the environment variable _Log4jContextSelector_ to _org.apache.logging.log4j.core.async.AsyncLoggerContextSelector_ to enable asynchronous logging [for better performance.](http://logging.apache.org/log4j/2.0/manual/async.html)

Or set up and run in a [docker](https://www.docker.com) container using the provided dockerfile.
Once up and running the tresor-pdp provides (in its default state) a RESTful API on port 8080.

### RESTful API
* All URLs are relative to the base URL
* __UTF-8__ is assumed

| Resource                                                        | Description                        |
|:----------------------------------------------------------------|:-----------------------------------|
| [GET /](#discover-the-home-document)                            | discover the home document         |
| [POST /pdp](#retrieve-a-decision-from-the-pdp)                  | retrieve a decision from PDP       |
| [GET /policy/:clientID](#retrieve-all-policies-for-a-client)    | retrieve all policies for a client |
| [GET /policy/:clientID/:serviceID](#retrieve-a-specific-policy) | retrieve a specific policy         |
| [PUT /policy/:clientID/:serviceID](#put-a-policy)               | put a policy                       |
| [DELETE /policy/:clientID/:serviceID](#delete-a-policy)         | delete a policy                    |

#### Discover the Home document
~~~http
GET / HTTP/1.1
Accept: application/xml
~~~

---

#### Retrieve a decision from the PDP
##### XACML decision
~~~http
POST /pdp HTTP/1.1
Accept: application/xacml+xml
Content-Type: application/xacml+xml

:xacml-request
~~~
##### XACML-SAML decision
~~~http
POST /pdp HTTP/1.1
Accept: application/samlassertion+xml
Content-Type: application/samlassertion+xml

:xacml-saml-request
~~~

---

#### Retrieve all policies for a client
~~~http
GET /policy/:clientID/ HTTP/1.1

Accept: application/json
Authorization: BASIC <base64 encoded "username:password">
~~~

Returns a collection in the json-format:
~~~http
{ <service_id> : <policy> }
~~~

#### Retrieve a specific policy
~~~http
GET /policy/:clientID/:serviceID HTTP/1.1
Accept: application/xacml+xml
Authorization: BASIC <base64 encoded "username:password">
~~~

Returns:
~~~http
HTTP/1.0 200 OK
Content-Type: application/xacml+xml

:xacml_Policy
~~~

---

#### Put a policy
~~~http
PUT /policy/:clientID/:serviceID HTTP/1.1
Content-Type: application/xacml+xml
Authorization: BASIC <base64 encoded "username:password">

:xacml_policy
~~~

---

#### Delete a policy
~~~http
DELETE /policy/:clientID/:serviceID HTTP/1.1
Authorization: BASIC <base64 encoded "username:password">
~~~

## License
Licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
