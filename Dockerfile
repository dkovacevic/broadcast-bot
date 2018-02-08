FROM dejankovacevic/bots.runtime:latest

COPY target/channel.jar   /opt/channel/channel.jar
COPY channel.yaml         /etc/channel/channel.yaml

WORKDIR /opt/channel
