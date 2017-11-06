#!/usr/bin/python
#  -*- coding: utf-8 -*-


import os
import time
import sys
import abc
import random
import socket

from trpo import *

stateDim = 5
actionDim = 5
timestepLimit = 30


path = "/home/taoma/ASpace/eclipse_mars2/eclipse_mars2_space/moka_project/org.eclipse.papyrus.mosh/trpo_models4/"

trpo = TRPO(stateDim, actionDim, timestepLimit, path, True)


states = []
state = [[0, 0, 0, 0, 0]]
action = trpo.policy_net.get_policy(trpo.sess, state)[0]
print action

for n in xrange(10):
    for s in state:
        for i in xrange(len(action)):
            s[i] = s[i] + action[i]#(action[i] + 4.4)/10.8*2 - 1
    #print state
    states.append(state[0][:])
    action = trpo.policy_net.get_policy(trpo.sess, state)[0]
    print action

reward = 25.0;
index = 0
while index + 1 < 10:
    r = 0;
    for j in xrange(5):
        r += abs(states[i+1][j] - states[i][j]); 
    reward -= r;
    index += 2

reward /= 25
print reward


