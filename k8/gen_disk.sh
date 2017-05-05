#!/bin/bash

NAME="channel-disk"

gcloud compute disks create $NAME \
    --zone europe-west1-c \
    --size 10GB \
    --type pd-ssd