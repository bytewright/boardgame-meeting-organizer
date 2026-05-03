# Update dep.tree

`./mvnw dependency:tree -Doutput=dep.tree`

# build and push

`docker build -t <repo>/bgmo:latest .`
`docker push <repo>/bgmo:latest`
