#!/bin/bash

counter=1
while [ $counter -le 1000 ]
do
   kafka-topics --create --zookeeper zookeeper-0:2181 --replication-factor 1 --partitions 12 --topic topic_e_$counter
   ((counter++))
done
