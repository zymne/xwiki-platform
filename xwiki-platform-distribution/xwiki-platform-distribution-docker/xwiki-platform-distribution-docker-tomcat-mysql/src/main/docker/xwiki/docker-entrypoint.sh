#!/bin/bash
# ---------------------------------------------------------------------------
# See the NOTICE file distributed with this work for additional
# information regarding copyright ownership.
#
# This is free software; you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 2.1 of
# the License, or (at your option) any later version.
#
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this software; if not, write to the Free
# Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
# 02110-1301 USA, or see the FSF site: http://www.fsf.org.
# ---------------------------------------------------------------------------

set -e

function first_start() {
  configure
  touch /usr/local/tomcat/webapps/ROOT/.first_start_completed
}

function other_starts() {
  mkdir -p /usr/local/xwiki/data
  restoreConfigurationFile 'hibernate.cfg.xml'
  restoreConfigurationFile 'xwiki.cfg'
  restoreConfigurationFile 'xwiki.properties'
}

# $1 - the line to add to the end of the xwiki.cfg file
function xwiki_add_cfg() {
    printf "    Adding: ${1}\n"
    printf "${1}\n" >> /usr/local/tomcat/webapps/ROOT/WEB-INF/xwiki.cfg
}

# $1 - the line to add to the end of the xwiki.properties file
function xwiki_add_properties() {
    printf "    Adding: ${1}\n"
    printf "${1}\n" >> /usr/local/tomcat/webapps/ROOT/WEB-INF/xwiki.properties
}

# Allows to use sed but with user input which can contain special sed characters such as \, / or &. Also supports tab.
# $1 - the text to search for
# $2 - the replacement text
# $3 - the file in which to do the search/replace
function safesed {
    sed -i "s/$(echo "$1" | sed -e 's/\([[\/.*]\|\]\)/\\&/g' | sed -e 's:\t:\\t:g')/$(echo "$2" | sed -e 's/[\/&]/\\&/g' | sed -e 's:\t:\\t:g')/g" "$3"
}

# $1 - the config file name found in WEB-INF (e.g. "xwiki.cfg")
function saveConfigurationFile() {
  if [ -f "/usr/local/xwiki/data/$1" ]; then
     echo "  Reusing existing config file $1..."
     cp "/usr/local/xwiki/data/$1" "/usr/local/tomcat/webapps/ROOT/WEB-INF/$1"
  else
     echo "  Saving config file $1..."
     cp "/usr/local/tomcat/webapps/ROOT/WEB-INF/$1" "/usr/local/xwiki/data/$1"
  fi
}

# $1 - the config file name to restore in WEB-INF (e.g. "xwiki.cfg")
function restoreConfigurationFile() {
  if [ -f "/usr/local/xwiki/data/$1" ]; then
     echo "  Synchronizing config file $1..."
     cp "/usr/local/xwiki/data/$1" "/usr/local/tomcat/webapps/ROOT/WEB-INF/$1"
  else
     echo "  No config file $1 found, using default from container..."
     cp "/usr/local/tomcat/webapps/ROOT/WEB-INF/$1" "/usr/local/xwiki/data/$1"
  fi
}

function configure() {
  # All ${VAR} constructs point to environment variables passed to "docker run" as in:
  #   docker run -it -e "DB_USER=xwiki" -e "DB_PASSWORD=xwiki"
  # Note that we always provide default values when no value is passed

  echo 'Configuring XWiki...'

  echo '  Updating hibernate.cfg.xml...'

  safesed "@replaceuser@" ${DB_USER:-xwiki} /usr/local/tomcat/webapps/ROOT/WEB-INF/hibernate.cfg.xml
  safesed "@replacepassword@" ${DB_PASSWORD:-xwiki} /usr/local/tomcat/webapps/ROOT/WEB-INF/hibernate.cfg.xml
  safesed "@replacecontainer@" ${DB_HOST:-db} /usr/local/tomcat/webapps/ROOT/WEB-INF/hibernate.cfg.xml
  safesed "@replacedatabase@" ${DB_DATABASE:-xwiki} /usr/local/tomcat/webapps/ROOT/WEB-INF/hibernate.cfg.xml
  safesed "@replaceextra@" "${DB_EXTRA:-}" /usr/local/tomcat/webapps/ROOT/WEB-INF/hibernate.cfg.xml

  echo '  Updating xwiki.cfg...'

  ## Using filesystem-based attachments
  xwiki_add_cfg 'xwiki.store.attachment.hint = file'
  xwiki_add_cfg 'xwiki.store.attachment.versioning.hint = file'
  xwiki_add_cfg 'xwiki.store.attachment.recyclebin.hint = file'

  ## Generating authentication validation and encryption keys
  xwiki_add_cfg "xwiki.authentication.validationKey = $(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)"
  xwiki_add_cfg "xwiki.authentication.encryptionKey = $(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)"

  ## Adding extra properties passed by the user
  if [ ! -z ${EXTRA_CFG+x} ]; then
    xwiki_add_cfg "${EXTRA_CFG}"
  fi

  echo '  Updating xwiki.properties...'

  ## Setting permanent directory
  xwiki_add_properties 'environment.permanentDirectory = /usr/local/xwiki/data'

  ## Start libreoffice automatically
  xwiki_add_properties 'openoffice.autoStart = true'

  ## Adding extra properties passed by the user
  if [ ! -z ${EXTRA_PROPERTIES+x} ]; then
    xwiki_add_properties "${EXTRA_PROPERTIES}"
  fi

  # If the files already exist then copy them to the XWiki's WEB-INF directory. Otherwise copy the default config
  # files to the permanent directory so that they can be easily modified by the user. They'll be synced at the next
  # start.
  mkdir -p /usr/local/xwiki/data
  saveConfigurationFile 'hibernate.cfg.xml'
  saveConfigurationFile 'xwiki.cfg'
  saveConfigurationFile 'xwiki.properties'
}

# This if will check if the first argument is a flag but only works if all arguments require a hyphenated flag
# -v; -SL; -f arg; etc will work, but not arg1 arg2
if [ "${1:0:1}" = '-' ]; then
    set -- xwiki "$@"
fi

# Check for the expected command
if [ "$1" = 'xwiki' ]; then
  if [[ ! -f /usr/local/tomcat/webapps/ROOT/.first_start_completed ]]; then
    first_start
  else
    other_starts
  fi
  shift
  set -- catalina.sh run "$@"
fi

# Else default to run whatever the user wanted like "bash"
exec "$@"
