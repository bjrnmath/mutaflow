#!/bin/sh

#This source code is part of the MutaFlow project. It runs the analysis pipeline.
#Copyright (C) 2017  Björn Mathis

#This program is free software: you can redistribute it and/or modify
#it under the terms of the GNU General Public License as published by
#the Free Software Foundation, either version 3 of the License, or
#(at your option) any later version.

#This program is distributed in the hope that it will be useful,
#but WITHOUT ANY WARRANTY; without even the implied warranty of
#MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#GNU General Public License for more details.

#You should have received a copy of the GNU General Public License
#along with this program.  If not, see <https://www.gnu.org/licenses/>.

rm -rf AppOutSave

mkdir AppOutSave

python3 instrument_parallel.py

sh SignAndAlign.sh

java -jar Runner.jar

sh startParser.sh

java -jar FlowExtractor.jar .