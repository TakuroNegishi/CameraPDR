#!/bin/sh
# AUTO-GENERATED FILE, DO NOT EDIT!
if [ -f $1.org ]; then
  sed -e 's!^C:/Cygwin/lib!/usr/lib!ig;s! C:/Cygwin/lib! /usr/lib!ig;s!^C:/Cygwin/bin!/usr/bin!ig;s! C:/Cygwin/bin! /usr/bin!ig;s!^C:/Cygwin/!/!ig;s! C:/Cygwin/! /!ig;s!^D:!/cygdrive/d!ig;s! D:! /cygdrive/d!ig;s!^C:!/cygdrive/c!ig;s! C:! /cygdrive/c!ig;' $1.org > $1 && rm -f $1.org
fi
