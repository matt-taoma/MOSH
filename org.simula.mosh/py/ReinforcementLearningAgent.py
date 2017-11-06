#!/usr/bin/python
#  -*- coding: utf-8 -*-


import os
import time
import sys
import abc
import random
import socket

from trpo import *

class EnvironmentConnector:
    def __init__(self):
        address = ('127.0.0.1', 8086)
        self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.s.connect(address)
        self.cmds = []

    def sendMsg(self, msg):
        self.s.send(msg + "$")

    def receiveMsg(self):
        if len(self.cmds) > 0:
            return self.cmds.pop(0)
            
        msg = ''
        while True:
            msg += self.s.recv(4096)
            if msg.endswith('$'):
                break
        items = msg.split("$")
        for item in items:
            if len(item) != 0:
                self.cmds.append(item)
        return self.cmds.pop(0)

    def request(self, msg):
        self.s.send(msg + "$")
        return self.receiveMsg()

class EnvironmentProxy:

    def __init__(self):
        self.stop = False

        self.connector = EnvironmentConnector()

        res = self.connector.request("get stateDim");
        self.stateDim = int(res)
        res = self.connector.request("get actionDim");
        self.actionDim = int(res)
        res = self.connector.request("get timestepLimit")
        self.timestepLimit = int(res)
        self.train_dir = self.connector.request("get train dir")
        res = self.connector.request("done")

        self.paths = []
        self.curPath = {"obs": [], "action": [], "dist_n": [], "reward": []}

        self.trpo = TRPO(self.stateDim, self.actionDim, self.timestepLimit, self.train_dir)

        self.tryWithoutNoise = -1

    def interact(self):

        res = self.connector.receiveMsg()

        if res == 'next epoch':
            res = self.connector.request("get state")
            state = self.parseState(res)
            action = self.epoch(state)
            res = self.connector.request('forward action ' + self.actionStr(action))
            return True
        elif res == 'episode finish':
            res = self.connector.request("get reward")
            reward = float(res)
            self.episodeFinish(reward)
            return False
        elif res == 'iteration finish':
            self.trpo.save()
            self.stop = True
            return False

    def epoch(self, state):
        withoutNoise = False
        if self.tryWithoutNoise >=0 :
            withoutNoise = True
            self.tryWithoutNoise -= 1
        action, dist_n = self.trpo.next_action(state, withoutNoise)

        if withoutNoise:
            print "\n\n\n"
            print state
            print action
            print dist_n
            print "\n\n\n"

        self.curPath["obs"].append(state)
        self.curPath["action"].append(action)
        self.curPath["dist_n"].append(dist_n)
        return action

    def episodeFinish(self, reward):
        for i in xrange(len(self.curPath["obs"])):
            self.curPath["reward"].append(reward)
        self.paths.append(self.curPath)
        self.curPath = {"obs": [], "action": [], "dist_n": [], "reward": []}

        if (len(self.paths) * 10) % rollout_timesteps == 0:
            self.train()
            
            self.paths = []

            print "step:"
            print(self.trpo.policy_net.global_step.eval(session=self.trpo.sess))

            

    def parseState(self, str):
        items = str.split(', ')
        return [float(item) for item in items]

    def actionStr(self, action):
        return ', '.join([str(v) for v in action])

    def train(self):
        self.trpo.train(self.paths)

envProxy = EnvironmentProxy()

if not os.path.isdir(envProxy.train_dir):
    os.makedirs(envProxy.train_dir)

while not envProxy.stop:
    while envProxy.interact():
        pass



