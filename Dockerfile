FROM dejankovacevic/bots.runtime:2.10.0

COPY target/channel.jar   /opt/channel/channel.jar
COPY channel.yaml         /etc/channel/channel.yaml

WORKDIR /opt/channel
