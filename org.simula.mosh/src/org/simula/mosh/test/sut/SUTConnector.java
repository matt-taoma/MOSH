/*****************************************************************************
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Simula Research Lab, Norway 
*
*****************************************************************************/


package org.simula.mosh.test.sut;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class SUTConnector {

	public enum MsgType {
		MSG_TYPE_COMPLETE, MSG_TYPE_STATUS
	}

	private String host;
	private int port;
	private SocketChannel channel = null;

	private Selector selector;

	private ByteBuffer inputBuffer;

	private String previousStr;

	public SUTConnector(String host, String port) {
		this.host = host;
		this.port = Integer.parseInt(port);

		inputBuffer = ByteBuffer.allocate(1024);
		previousStr = "";

		try {
			selector = Selector.open();
			channel = SocketChannel.open();
			channel.configureBlocking(false);

			channel.connect(new InetSocketAddress(host, this.port));
		}
		catch (IOException e) {
			System.err.println(e);
		}
	}

	public void close() {
		if (channel == null) {
			return;
		}
		try {
			channel.shutdownOutput();
			channel.close();

		}
		catch (IOException e) {
			channel = null;
		}

		try {
			selector.close();
		}
		catch (IOException e) {
			selector = null;
		}

	}

	public boolean send(String str) {

		boolean res = false;
		try {
			res = _send(str);
		}
		catch (ClosedChannelException e) {
			try {
				if(!reconnect()){
					return false;
				}
				res = _send(str);
			}
			catch (IOException e1) {
				System.err.println(e1);
			}
		}
		catch (IOException e) {
			System.err.println(e);
		}
		
		return res;

	}

	private boolean _send(String str) throws IOException {

		if (channel.isConnectionPending()) {
				channel.finishConnect();
		}

		ByteBuffer buf = null;
		try {
			buf = ByteBuffer.wrap(str.getBytes("utf-8"));
		}
		catch (UnsupportedEncodingException e1) {
			return false;
		}
		while (buf.hasRemaining()) {
			channel.write(buf);
		}

		return true;
	}

	private boolean reconnect() throws IOException {

		channel = SocketChannel.open();
		channel.configureBlocking(false);

		channel.register(selector, SelectionKey.OP_CONNECT);
		channel.connect(new InetSocketAddress(host, port));

		int tries = 0;
		while (true) {

			selector.select(1000);

			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

			while (keys.hasNext()) {
				SelectionKey key = keys.next();
				keys.remove();

				if (!key.isValid())
					continue;

				if (key.isConnectable()) {
					if (channel.isConnectionPending()) {
						channel.finishConnect();
					}
					channel.configureBlocking(false);
				}
			}
			if (channel.isConnected()) {
				break;
			}
			if (tries > 5) {
				return false;
			}
			tries++;
		}

		return true;

	}

	private String read(MsgType type) throws IOException {

		String res = null;

		inputBuffer.clear();
		int n = 0;
		int fails_num = 0;
		while ((n = channel.read(inputBuffer)) >= 0) {

			if (fails_num > 3) {
				break;
			}

			if (n == 0) {
				fails_num++;
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}

			int p = inputBuffer.position();

			if (type.equals(MsgType.MSG_TYPE_COMPLETE)) {
				if (p < 8) {
					continue;
				}

				while (p > 0) {
					p--;
					byte lastChar = inputBuffer.get(p);
					if (lastChar != (byte) ' ' && lastChar != (byte) 'C' && lastChar != (byte) 'O'
							&& lastChar != (byte) 'M' && lastChar != (byte) 'P' && lastChar != (byte) 'L'
							&& lastChar != (byte) 'E' && lastChar != (byte) 'T') {
						break;
					}
				}

				if (p <= 0) {
					// reach here means all recived str are
					// "COMPLETE"
					res = "COMPLETE";
				}
				previousStr = new String(inputBuffer.array(), Charset.forName("UTF-8")).trim();
				break;
			}
			else if (type.equals(MsgType.MSG_TYPE_STATUS)) {

				while (p > 0) {
					p--;
					byte lastChar = inputBuffer.get(p);
					if (lastChar != (byte) ' ' && lastChar != (byte) 'C' && lastChar != (byte) 'O'
							&& lastChar != (byte) 'M' && lastChar != (byte) 'P' && lastChar != (byte) 'L'
							&& lastChar != (byte) 'E' && lastChar != (byte) 'T') {
						break;
					}
				}

				if (p <= 0) {
					continue;
				}

				byte lastChar = inputBuffer.get(p);
				if (lastChar == (byte) '&') {

					res = new String(inputBuffer.array(), Charset.forName("UTF-8")).trim();
					int i = res.lastIndexOf('&');
					String extra = res.substring(i);
					res = res.substring(0, i);

					i = res.indexOf('!');
					if (i < 0) {
						res = previousStr + res;
						i = res.indexOf('!');
					}

					if (i < 0) {
						System.err.println("receive error.. " + res);
					}
					else {
						res = res.substring(i, res.length());
					}

					previousStr = extra;

					break;
				}
				else {
					continue;
				}

			}
			

		}
		inputBuffer.flip();
		Arrays.fill(inputBuffer.array(), (byte) 0);
		return res;
	}

	public String receive(MsgType type) {

		String res = null;
		try {
			res = _receive(type);
		}
		catch (ClosedChannelException e) {
			
			try {
				if(reconnect()){
					res = _receive(type);
				}
			}
			catch (IOException e1) {
				System.err.println(e1);
			}
		}
		catch (IOException e) {
			System.err.println(e);
		}
		return res;
	}

	private String _receive(MsgType type) throws IOException {

		if (channel.isConnectionPending()) {
			channel.finishConnect();
		}

		channel.register(selector, SelectionKey.OP_READ);
		int tries = 0;
		while (true) {
			selector.select(1000);
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey key = keys.next();
				keys.remove();

				if (!key.isValid())
					continue;

				if (key.isReadable()) {
					String res = read(type);
					if(res != null){
						return res;
					}
				}
			}
			if (tries > 5) {
				return null;
			}
			tries++;
		}

	}

}