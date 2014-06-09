/*
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.amoeba.manager.io;

import java.nio.ByteBuffer;

import com.meidusa.amoeba.manager.ManagerConstant;
import com.meidusa.amoeba.net.io.PacketOutputStream;

/**
 * 
 * <b> The Packet Header </b>
 * 
 * <pre>
 * Bytes                 Name
 *  -----                 ----
 *  3                     Packet Length
 *  1                     Packet TYPE
 * </p>
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 * 
 */
public class ManagerPacketOutputStream extends PacketOutputStream implements ManagerConstant{
	protected boolean packetwrittenWithHead;
	
	/**
	 * 
	 * @param packetwrittenWithHead д���ݵ�ʱ���Ƿ�д���ͷ��Ϣ��true--��ʾд�����ݵ�ʱ���Ѿ������˰�ͷ��Ϣ��
	 * 							   ��������Ҫ�ڵ���{@link #returnPacketBuffer()} ��ʱ����Ҫʵʱ���ɰ�ͷ��Ϣ
	 */
	public ManagerPacketOutputStream(boolean packetwrittenWithHead){
		this.packetwrittenWithHead = packetwrittenWithHead;
		resetPacket();
	}
	public ByteBuffer returnPacketBuffer ()
    {
        // flip the buffer which will limit it to it's current position
        _buffer.flip();
        if(!packetwrittenWithHead){
	        /**
	         *  ��ͷ��Ϣ�����ȣ����ǲ�������ͷ����
	         */
	        int count = _buffer.limit()-HEADER_PAD.length;
	        _buffer.put((byte)(count & 0xff));
	        _buffer.put((byte) (count >> 8));
	        _buffer.put((byte) (count >> 16));
	        _buffer.put((byte) (count >> 24));
        }
        _buffer.rewind();
        return _buffer;
    }
	
	/**
	 * 
	 */
	protected void initHeader(){
		if(!packetwrittenWithHead){
			_buffer.put(HEADER_PAD);
		}
    }
}
