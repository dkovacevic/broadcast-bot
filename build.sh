#!/usr/bin/env bash
mvn package -DskipTests=true -Dmaven.javadoc.skip=true
docker build -t dejankovacevic/channel-bot:latest .
docker push dejankovacevic/channel-bot
kubectl delete pod -l name=channel -n prod
kubectl get pods -l name=channel -n prod

