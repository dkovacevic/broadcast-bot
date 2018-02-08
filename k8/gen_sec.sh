#!/bin/bash

SECRET_NAME="channel"
AUTH_TOKEN="your_token"

kubectl delete secret $SECRET_NAME
kubectl create secret generic $SECRET_NAME --from-literal=token=$AUTH_TOKEN
