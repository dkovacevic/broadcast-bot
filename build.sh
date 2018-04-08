#!/usr/bin/env bash
mvn package -DskipTests=true -Dmaven.javadoc.skip=true
docker build -t dejankovacevic/channel-bot:0.7.0 .
docker push dejankovacevic/channel-bot
kubectl delete pod -l name=channel
kubectl get pods -l name=channel

