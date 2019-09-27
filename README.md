# entando-plugin-sidecar

This application is intended to be used as a sidecar for the Entando Plugins that need to manage connection
configurations and credentials (create, update, retrieve and delete). However, Entando Plugins don't need 
to call the endpoints directly. Instead, it will be done by the Connection Config Connector 
(https://github.com/entando/connection-config-connector).

The connection configurations and credentials managed by this application are stored as Kubernetes Secrets in a
Kubernetes Cluster.

## Running Integration tests

Integration tests require a Kubernetes environment. Having Minikube installed and configured is enough for this purpose.
Make sure a namespace is set up in your context. It can be done with the following command:
```bash
kubectl config set-context --current --namespace=default
```
