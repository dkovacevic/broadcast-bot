#!/bin/bash

NAME="channel-config"

kubectl delete configmap $NAME
kubectl create configmap $NAME --from-file=../conf