#!/bin/sh
sudo -S docker exec -i tender-mysql-1 mysql -uschool -pschoolpass --table school_portal < /tmp/webhook-setup.sql
