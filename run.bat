@echo off
adb root
adb push daemon.dex /data/data
adb shell app_process -Djava.class.path=/data/data/daemon.dex  / me.aw.uia.Daemon "我爱中国" setClip




