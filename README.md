# TRESOR XACML Policy Decision Point

The tresor-pdp is an XACML Policy Decision Point (PDP) created in the context of the [TRESOR project](http://www.cloud-tresor.de) which supports XACML2, XACML3 and geoXACML for both though non-standard in the case of XACML3.

[Balana](https://www.github.com/wso2/balana) is used as a base for decision processing, extended by geoXACML components from [geotools](https://github.com/geotools/geotools) community and specific policystore implementations (among other extensions) as well as a contexthandler providing a RESTful API.

## Deployment

### Option 1: Package with maven and run with java
```shell
# clone the repo
git clone https://github.com/TU-Berlin-SNET/tresor-pdp.git

# change into the directory
cd tresor-pdp

# package with maven
mvn package

# run it
java -jar -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector modules/contexthandler/target/tresor-pdp.jar
```
It is __strongly recommended__ to set the system property _Log4jContextSelector_ to _org.apache.logging.log4j.core.async.AsyncLoggerContextSelector_ to [enable asynchronous logging for all loggers for better performance.](http://logging.apache.org/log4j/2.0/manual/async.html)

### Option 2: Deploy with [docker](https://www.docker.com/)
```shell
# clone the repo
git clone https://github.com/TU-Berlin-SNET/tresor-pdp.git

# change into the directory
cd tresor-pdp

# build the image from dockerfile
docker build -t tresor-pdp .

# run it
docker run --name="tresor-pdp" -p 8080:8080 tresor-pdp
```

## Configuration
Configuration for the tresor-pdp can be provided in yaml or, in a very limited way, through the command line. 

### Command Line
These arguments can be provided on start:
```shell
--server.port=PORT                  binds server instance to port  
--spring.config.location=PATH       location of .yml config file to load  
--policystore.path=PATH             stores policies in given directory path, overriding any other settings  
```

### application.yml
By default, the tresor-pdp uses the application.yml configuration file example in modules/contexthandler/src/main/resources/ which looks similar to this.
```
#
# Usage:
#
# pdp:                        # prefix for tresor-pdp config
#   locationpips:             # (optional) configure locationpips
#     -                          
#       url: 
#       authentication:       # (optional) http-basic authentication header
#   stationpips:              # (optional) configure stationpips
#     -
#       url: <url>               
#   policystore:              # (optional) configure policystore
#     type: (file or redis)
#     path:                   # (optional) file path, default: pdp directory
#     host:                   # (optional) redisDB host, default: localhost
#     port:                   # (optional) redisDB port, default: 6379
#     timeout:                # (optional) redisDB timeout in ms, default: 2000
#     password:               # (optional) redisDB password

pdp:
  locationpips:
    -
      url: localhost
      authentication: Basic dGhpc19pczpub3R0aGVwYXNzd29yZA==
  policystore:
    type: file
```

## RESTful API
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

### Discover the Home document
```http
GET / HTTP/1.1
Accept: application/xml
```

### Retrieve a decision from the PDP
#### XACML decision
```http
POST /pdp HTTP/1.1
Accept: application/xacml+xml
Content-Type: application/xacml+xml

:xacml-request
```
#### XACML-SAML decision
```http
POST /pdp HTTP/1.1
Accept: application/samlassertion+xml
Content-Type: application/samlassertion+xml

:xacml-saml-request
```

### Retrieve all policies for a client
```http
GET /policy/:clientID/ HTTP/1.1

Accept: application/json
Authorization: BASIC <base64 encoded "username:password">
```
Returns a collection in the json-format:
```http
{ "service_id" : "policy" }
```

### Retrieve a specific policy
```http
GET /policy/:clientID/:serviceID HTTP/1.1
Accept: application/xacml+xml
Authorization: BASIC <base64 encoded "username:password">
```
Returns:
```http
HTTP/1.0 200 OK
Content-Type: application/xacml+xml

:xacml_Policy
```

### Put a policy
```http
PUT /policy/:clientID/:serviceID HTTP/1.1
Content-Type: application/xacml+xml
Authorization: BASIC <base64 encoded "username:password">

:xacml_policy
```

### Delete a policy
```http
DELETE /policy/:clientID/:serviceID HTTP/1.1
Authorization: BASIC <base64 encoded "username:password">
```

## License
Licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
