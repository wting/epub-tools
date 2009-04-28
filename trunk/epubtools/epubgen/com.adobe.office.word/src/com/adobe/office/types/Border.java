/*******************************************************************************
* Copyright (c) 2009, Adobe Systems Incorporated
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without 
* modification, are permitted provided that the following conditions are met:
*
* ·        Redistributions of source code must retain the above copyright 
*          notice, this list of conditions and the following disclaimer. 
*
* ·        Redistributions in binary form must reproduce the above copyright 
*		   notice, this list of conditions and the following disclaimer in the
*		   documentation and/or other materials provided with the distribution. 
*
* ·        Neither the name of Adobe Systems Incorporated nor the names of its 
*		   contributors may be used to endorse or promote products derived from
*		   this software without specific prior written permission. 
* 
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
* ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
* OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
* THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*******************************************************************************/

package com.adobe.office.types;

public class Border {

	BorderSide top;
	BorderSide bottom;
	BorderSide left;
	BorderSide right;
	BorderSide insideH;
	BorderSide insideV;
	
	public BorderSide getInsideH() {
		return insideH;
	}

	public Border() {
	}
	
	public BorderSide getBottom() {
		return bottom;
	}

	public void setBottom(BorderSide bottom) {
		this.bottom = bottom;
	}

	public BorderSide getLeft() {
		return left;
	}

	public void setLeft(BorderSide left) {
		this.left = left;
	}

	public BorderSide getRight() {
		return right;
	}

	public void setRight(BorderSide right) {
		this.right = right;
	}

	public BorderSide getTop() {
		return top;
	}

	public void setTop(BorderSide top) {
		this.top = top;
	}

	public void setInsideH(BorderSide insideH) {
		this.insideH = insideH;
	}

	public BorderSide getInsideV() {
		return insideV;
	}

	public void setInsideV(BorderSide insideV) {
		this.insideV = insideV;
	}
	
}
