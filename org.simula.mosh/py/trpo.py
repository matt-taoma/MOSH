#!/usr/bin/python
#  -*- coding: utf-8 -*-

from common import *

value_lr = 5e-4
max_kl = 1
rollout_timesteps = 1e4

policy_net_hiden_units = 10
value_net_hiden_units = 10

class PolicyNet(object):
    def __init__(self, scope, state_dim, action_dim):
        self.action_dim = action_dim
        with tf.device("/cpu:0"):
            with tf.variable_scope(scope):
                # placeholder
                self.state = tf.placeholder(tf.float64, shape=[None, state_dim], name="state")
                self.action = tf.placeholder(tf.float64, shape=[None, action_dim], name="action")
                self.advantage = tf.placeholder(tf.float64, shape=[None], name="advantage")
                self.old_dist_n = tf.placeholder(tf.float64, shape=[None, action_dim * 2], name="old_dist_n")
                # mlp
                h1, self.w1, self.b1 = full_connect(self.state, (state_dim, policy_net_hiden_units), "fc1", with_param=True)
                h2, self.w2, self.b2 = full_connect(h1, (policy_net_hiden_units, policy_net_hiden_units), "fc2", with_param=True)
                h3, self.w3, self.b3 = full_connect(h2, (policy_net_hiden_units, action_dim), "fc3", with_param=True, activate="sigmoid")
                self.logits = tf.identity(h3, name="policy_out")
                # self.dist_n = tf.concat([h3/2 + 0.25, tf.exp(h3)/13.5], 1, name="dist_n")
                self.dist_n = tf.concat([h3, tf.exp(h3)], 1, name="dist_n")
                # log likelihood
                old_logp_n = self.log_likelihood(self.action, self.old_dist_n)
                logp_n = self.log_likelihood(self.action, self.dist_n)
                # loss
                self.surrogate_loss = - tf.reduce_mean(tf.exp(logp_n - old_logp_n) * self.advantage, name="sur_loss")
                self.entropy_loss = tf.reduce_mean(self.entropy(self.dist_n), name="entropy_loss")
                self.kl_loss = tf.reduce_mean(self.kl_divergence(self.old_dist_n, self.dist_n), name="kl_loss")
                self.kl_first_fixed = tf.reduce_mean(self.kl_divergence(tf.stop_gradient(self.dist_n), self.dist_n),
                                                     name="kl_first_fixed")
                kl_grads = tf.gradients(self.kl_first_fixed, self.get_vars(), name="kl_ff_grads")
                pi_grads = tf.gradients(self.surrogate_loss, self.get_vars(), name="policy_grads")
                self.pi_grads = self.get_flat(pi_grads)
                # fisher vector product  (gvp: gradient vector product, fvp: fisher vector product)
                self.flat_theta = tf.placeholder(tf.float64, shape=[None], name="flat_tangent")
                tangent = self.set_from_flat(self.flat_theta)
                gvp = [tf.reduce_sum(tg * kg) for tg, kg in zip(tangent, kl_grads)]
                kl_sen_grads = tf.gradients(gvp, self.get_vars(), name="kl_sen_grads")
                self.fvp = self.get_flat(kl_sen_grads)
                # gf: get flat,  sff: set from flat
                self.gf_vars = self.get_flat(self.get_vars())
                sff_vars = [tf.assign(var, t) for t, var in zip(self.set_from_flat(self.flat_theta), self.get_vars())]
                self.sff_vars_op = tf.group(*sff_vars)
                # summary
                summaries = list()
                summaries.append(tf.summary.scalar("surrogate_loss", self.surrogate_loss))
                summaries.append(tf.summary.scalar("entropy_loss", self.entropy_loss))
                summaries.append(tf.summary.scalar("kl_loss", self.kl_loss))
                self.summary_op = tf.summary.merge(summaries, name="policy_summary_op")
                # set global step
                self.global_step = tf.get_variable("value_net_global_step", shape=[],
                                                   initializer=tf.constant_initializer(0), trainable=False)
                self.augment_flag = tf.placeholder(tf.bool, shape=[], name="augment_global_step")
                self.augment_step_op = tf.assign(self.global_step,
                                                 tf.where(self.augment_flag, self.global_step + 1, self.global_step))

    def train(self, paths, sess, summary_writer):
        feed_dict = {
            self.state: paths["states"],
            self.action: paths["actions"],
            self.advantage: paths["advant"],
            self.old_dist_n: paths["dist_n"]
        }

        def fisher_vector_product(g):
            feed_dict[self.flat_theta] = g
            return sess.run(self.fvp, feed_dict=feed_dict)

        def compute_loss(theta):
            sess.run(self.sff_vars_op, feed_dict={self.flat_theta: theta})
            return sess.run(self.surrogate_loss, feed_dict=feed_dict)

        # policy gradients
        pi_grads = sess.run(self.pi_grads, feed_dict=feed_dict)
        # flatten vars
        theta_prev = sess.run(self.gf_vars)
        # conjugate gradient
        step_dir = self.conjugate_gradient(fisher_vector_product, -pi_grads)
        # lagrange multiplier
        lm = np.sqrt(0.5 * step_dir.dot(fisher_vector_product(step_dir)) / max_kl)
        full_step = step_dir / lm
        expected_improve_rate = -pi_grads.dot(step_dir) / lm
        theta_new = self.line_search(compute_loss, theta_prev, full_step, expected_improve_rate)
        sess.run([self.sff_vars_op, self.augment_step_op], feed_dict={self.flat_theta: theta_new,
                                                                      self.augment_flag: True})
        summary_str = sess.run(self.summary_op, feed_dict=feed_dict)

        summary_writer.add_summary(summary_str, self.global_step.eval(session=sess))

    def get_flat(self, grads):
        flat_grads = []
        for grad, var in zip(grads, self.get_vars()):
            flat_grads.append(tf.reshape(grad, shape=[np.prod(var.get_shape().as_list())]))
        theta = tf.concat(flat_grads, 0)
        return theta

    def set_from_flat(self, theta):
        grads = []
        start_size = 0
        for var in self.get_vars():
            var_size = np.prod(var.get_shape().as_list())
            grad = tf.reshape(theta[start_size: (start_size + var_size)], shape=var.get_shape().as_list())
            grads.append(grad)
            start_size += var_size
        return grads

    def conjugate_gradient(self, fvp_func, b, cg_iters=10, residual_tol=1e-10):
        p = b.copy()
        r = b.copy()
        x = np.zeros_like(b)
        rdotr = r.dot(r)
        for i in xrange(cg_iters):
            z = fvp_func(p)
            v = rdotr / p.dot(z)
            x += v * p
            r -= v * z
            newrdotr = r.dot(r)
            mu = newrdotr / rdotr
            p = r + mu * p
            rdotr = newrdotr
            if rdotr < residual_tol:
                break
        return x

    def line_search(self, loss_func, theta, full_step, expected_improve_rate):
        accept_ratio = .1
        max_backtracks = 10
        sff_val = loss_func(theta)
        for (_n_backtracks, step_frac) in enumerate(.5 ** np.arange(max_backtracks)):
            theta_new = theta + step_frac * full_step
            new_sff_val = loss_func(theta_new)
            actual_improve = sff_val - new_sff_val
            expected_improve = expected_improve_rate * step_frac
            ratio = actual_improve / expected_improve
            if ratio > accept_ratio and actual_improve > 0:
                return theta_new
        return theta

    def entropy(self, dist_prob):
        std = dist_prob[:, self.action_dim:]
        ent_const = tf.constant(0.5 * np.log(2 * np.pi * np.e) * self.action_dim, name="entropy_const", shape=[])
        return tf.reduce_sum(tf.log(std) + ent_const, reduction_indices=1)

    def log_likelihood(self, act, dist_prob):
        mean, std = dist_prob[:, :self.action_dim], dist_prob[:, self.action_dim:]
        log_const = 0.5 * np.log(2 * np.pi) * self.action_dim
        return -0.5 * tf.reduce_sum(tf.square(act - mean / std), reduction_indices=1) \
               - tf.reduce_sum(tf.log(std), reduction_indices=1) \
               - log_const

    def kl_divergence(self, old_dist_n, dist_n):
        mean_0 = old_dist_n[:, :self.action_dim]
        std_0 = old_dist_n[:, self.action_dim:]
        mean_1 = dist_n[:, :self.action_dim]
        std_1 = dist_n[:, self.action_dim:]
        return tf.reduce_sum(tf.log(std_1 / std_0), reduction_indices=1) + \
               tf.reduce_sum(tf.div(tf.square(std_0) + tf.square(mean_0 - mean_1), 2.0 * tf.square(std_1)),
                             reduction_indices=1) - \
               0.5 * self.action_dim

    def get_policy(self, sess, states):
        return sess.run(self.logits, feed_dict={self.state: states})

    def get_dist_n(self, sess, states):
        return sess.run(self.dist_n, feed_dict={self.state: states})

    def get_sample(self, sess, states, withoutNoise=False):
        dist_n = self.get_dist_n(sess, states)
        mean_nd = dist_n[:, :self.action_dim]
        std_nd = dist_n[:, self.action_dim:]
        if withoutNoise:
            action = []
            for v in mean_nd:
                v = v * 5.4 + 1  # 5.4 is 2 times of standard error, i.e., the range; 1 is mean
                action.append(v)
        else:
            action = (np.random.randn(len(states), self.action.shape[1]) * std_nd + mean_nd)
        # resAction = []
        # for v in action[0]:
        #     if v > 1:
        #         v = 1
        #     elif v < 0:
        #         v = 0
        #     resAction.append(v)
        # return [resAction], dist_n
        return action, dist_n

    def get_vars(self):
        return [self.w1, self.b1, self.w2, self.b2, self.w3, self.b3]


class ValueNet(object):
    def __init__(self, scope, state_dim, timestep_limit):
        self.timestep_limit = timestep_limit
        with tf.device("/cpu:0"):
            with tf.variable_scope(scope):
                # state input shape(None, state_dim + 1), the extra one is percent of time step
                self.state = tf.placeholder(tf.float64, shape=[None, state_dim + 1], name="state")
                self.target_q = tf.placeholder(tf.float64, shape=[None], name="target_q")
                self.global_step = tf.get_variable("value_net_global_step", shape=[],
                                                   initializer=tf.constant_initializer(0), trainable=False)
                # mlp
                h1, self.w1, self.b1 = full_connect(self.state, (state_dim + 1, value_net_hiden_units), "fc1", with_param=True,
                                                    weight_decay=0.0005)
                h2, self.w2, self.b2 = full_connect(h1, (value_net_hiden_units, value_net_hiden_units), "fc2", with_param=True,
                                                    weight_decay=0.0005)
                h3, self.w3, self.b3 = full_connect(h2, (value_net_hiden_units, 1), "fc3", with_param=True, activate=None,
                                                    weight_decay=0.0005)
                self.logits = tf.reshape(h3, shape=[-1], name="value_out")
                # losses
                l2_loss = tf.add_n(tf.get_collection("losses", scope=scope), name="l2_loss")
                
                self.mean_loss = tf.reduce_mean(tf.square(self.logits - self.target_q))
                
                self.cross_entropy_loss = tf.nn.sigmoid_cross_entropy_with_logits(labels=self.target_q, logits=self.logits, name="cross_entropy_loss")

                #self.loss = tf.reduce_sum(tf.square(self.logits - self.target_q)) + l2_loss
                self.loss = tf.reduce_mean(self.cross_entropy_loss)

                self.opt = tf.train.GradientDescentOptimizer(value_lr).minimize(self.cross_entropy_loss,
                                                                                      global_step=self.global_step)

                
                # summary op
                value_summary = tf.summary.scalar("value_loss", self.mean_loss)
                self.summary_op = tf.summary.merge([value_summary])

    def add_extra_dim(self, states):
        return np.concatenate([states, np.arange(len(states)).reshape(-1, 1) / float(self.timestep_limit)], axis=1)

    def get_value(self, sess, states):
        states = self.add_extra_dim(states)
        return sess.run(self.logits, feed_dict={self.state: states})

    def get_vars(self):
        return [self.w1, self.b1, self.w2, self.b2, self.w3, self.b3]

    def train(self, paths, session, summary_writer, n_iter=5000):
        for i in xrange(n_iter):
            fetches = [self.opt]
            fetches.append(self.mean_loss)
            if i % 10 == 0:
                fetches.append(self.summary_op)
            
            res = session.run(fetches, feed_dict={self.state: paths["states_with_timepercent"], self.target_q: paths["returns"]})
            
            mean_loss = float(res[1])
            if mean_loss < 0.00001:
                break

            if i % 10 == 0:
                summary_str = res[2]
                summary_writer.add_summary(summary_str, self.global_step.eval(session=session))
                print("mean loss:")
                print(mean_loss)

class TRPO(object):
    """paper: Trust Region Policy Optimization - arXiv.org"""

    def __init__(self, stateDim, actionDim, timestepLimit, train_dir, restore=False):
        
        self.timestep_limit = timestepLimit
        self.train_dir = train_dir
        # basic network
        self.policy_net = PolicyNet("policy_net", stateDim, actionDim)
        self.value_net = ValueNet("value_net", stateDim, timestepLimit)
        # session
        self.sess = tf.Session(graph=tf.get_default_graph(),
                               config=tf.ConfigProto(log_device_placement=False, allow_soft_placement=True))
        self.sess.run(tf.initialize_all_variables())
        
        self.summary_writer = tf.summary.FileWriter(train_dir, self.sess.graph)

        # saver
        saved_var_list = self.policy_net.get_vars() + self.value_net.get_vars() + \
                         [self.policy_net.global_step, self.value_net.global_step]
        self.saver = tf.train.Saver(max_to_keep=3, var_list=saved_var_list)
        
        if restore:
            restore_model(self.sess, train_dir, self.saver)

    def next_action(self, state, withoutNoise):
        action, dist_n = self.policy_net.get_sample(self.sess, [state], withoutNoise)
        return action[0], dist_n[0]


    def compute_advantage(self, paths):
        for path in paths:
            value_base = self.value_net.get_value(self.sess, path["obs"])
            path["returns"] = path["reward"] #here the reward is the same as the value
            path["advant"] = path["returns"] - value_base 
        # concat paths
        states = np.concatenate([path["obs"] for path in paths], axis=0)
        states_with_timepercent = np.concatenate([self.value_net.add_extra_dim(path["obs"]) for path in paths], axis=0)
        returns = np.concatenate([path["returns"] for path in paths], axis=0)
        action = np.concatenate([path["action"] for path in paths], axis=0)
        dist_n = np.concatenate([path["dist_n"] for path in paths], axis=0)
        advant = np.concatenate([path["advant"] for path in paths], axis=0)
        del paths
        paths = {"states": states, "states_with_timepercent": states_with_timepercent, "returns": returns, "actions": action, "dist_n": dist_n, "advant": advant}
        return paths

    def train(self, paths):
        
        # compute advantage
        paths = self.compute_advantage(paths)
        # fit value network
        self.value_net.train(paths, self.sess, self.summary_writer)
        # policy gradients
        self.policy_net.train(paths, self.sess, self.summary_writer)

        print(self.value_net.w1[0].eval(session=self.sess))

    def save(self):
        self.saver.save(self.sess, self.train_dir, global_step=self.policy_net.global_step)
        print(self.value_net.w1[0].eval(session=self.sess))
