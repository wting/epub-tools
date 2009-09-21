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
package com.adobe.dp.epub.conv;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class GUIDriver extends JFrame implements DropTargetListener, DragSourceListener {

	public static final long serialVersionUID = 0;

	BufferedImage epubIcon;

	Vector services = new Vector();

	DragSource dragSource = DragSource.getDefaultDragSource();

	Point dragStart = new Point(0, 0);

	Vector conversionQueue = new Vector();

	static class HighlightedFilter extends RGBImageFilter {
		public HighlightedFilter() {
			canFilterIndexColorModel = true;
		}

		public int filterRGB(int x, int y, int rgb) {
			return (((rgb & 0xFEFEFE) >> 1) + 0x80) | (rgb & 0xFF000000);
		}
	}

	class FileIcon extends JComponent implements DragGestureListener {

		public static final long serialVersionUID = 0;

		File file;

		Image icon;

		Image highlightedIcon;

		ConversionService service;

		boolean highlighted;

		boolean isAPrivateCopy;

		FileIcon(File file, Image icon, ConversionService service, boolean isAPrivateCopy) {
			this.file = file;
			this.icon = icon;
			this.isAPrivateCopy = isAPrivateCopy;
			this.service = service;
			setOpaque(false);
			setSize(34, 50);
			addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent evt) {
					highlighted = true;
					repaint();
				}
			});

			int actions = DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE;
			dragSource.createDefaultDragGestureRecognizer(this, actions, this);

			ImageFilter filter = new HighlightedFilter();
			ImageProducer producer = new FilteredImageSource(icon.getSource(), filter);
			highlightedIcon = createImage(producer);

		}

		void clearHighlight() {
			highlighted = false;
			repaint();
		}

		public void paint(Graphics g) {
			int width = getWidth();
			int height = 34;
			int iwidth = icon.getWidth(this);
			int iheight = icon.getHeight(this);
			int x = (width - iwidth) / 2;
			int y = (height - iheight) / 2;
			if (highlighted)
				g.drawImage(highlightedIcon, x, y, this);
			else
				g.drawImage(icon, x, y, this);
		}

		public void dragGestureRecognized(DragGestureEvent dge) {
			startDrag(this, dge);
		}
	}

	class FileTransferable implements Transferable {

		Vector list;

		FileTransferable(Vector list) {
			this.list = list;
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (flavor == DataFlavor.javaFileListFlavor)
				return list;
			throw new UnsupportedFlavorException(flavor);
		}

		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] flavors = { DataFlavor.javaFileListFlavor };
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor == DataFlavor.javaFileListFlavor;
		}

	}

	class Converter extends Thread implements ConversionClient {
		Converter() {
			super("Converter");
			setDaemon(true);
		}

		public void run() {
			while (true) {
				FileIcon item;
				synchronized (conversionQueue) {
					while (conversionQueue.size() == 0) {
						try {
							conversionQueue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					item = (FileIcon) conversionQueue.get(0);
					conversionQueue.removeElementAt(0);
				}
				reportProgress(0);
				final File res = item.service.convert(item.file, null, this);
				final FileIcon srcItem = item;
				if (res != null) {
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								FileIcon resItem = new FileIcon(res, epubIcon, null, true);
								getContentPane().add(resItem);
								resItem.setLocation(srcItem.getLocation());
								getContentPane().remove(srcItem);
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				reportProgress(1);
			}
		}

		public void reportIssue(String errorCode) {
		}

		public void reportProgress(float progress) {
		}
	}

	public GUIDriver() {
		setSize(200, 200);
		setLayout(null);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				GUIDriver.this.dispose();
			}
		});
		getContentPane().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent evt) {
				Component[] components = getContentPane().getComponents();
				for (int i = 0; i < components.length; i++) {
					if (components[i] instanceof FileIcon) {
						FileIcon fi = (FileIcon) components[i];
						fi.clearHighlight();
					}
				}
			}
		});
		new DropTarget(getContentPane(), this);
		try {
			services.add(Class.forName("com.adobe.dp.fb2.convert.FB2ConversionService").newInstance());
		} catch (Exception e) {
			e.printStackTrace();
		}

		InputStream png = GUIDriver.class.getResourceAsStream("epub.png");
		try {
			epubIcon = ImageIO.read(png);
		} catch (IOException e) {
			e.printStackTrace();
		}
		(new Converter()).start();
	}

	public void dragEnter(DropTargetDragEvent dtde) {
		dragOver(dtde);
	}

	public void dragExit(DropTargetEvent dte) {
	}

	public void dragOver(DropTargetDragEvent dtde) {
		if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			if (false) {
				DataFlavor[] flavors = dtde.getCurrentDataFlavors();
				if (flavors != null) {
					for (int i = 0; i < flavors.length; i++)
						System.out.println("Flavor: " + flavors[i]);
				}
			}
			return;
		}
		Transferable t = dtde.getTransferable();
		try {
			List files = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
			Iterator f = files.iterator();
			while (f.hasNext()) {
				File file = ((File) f.next());
				Iterator it = services.iterator();
				while (it.hasNext()) {
					ConversionService service = (ConversionService) it.next();
					if (service.canConvert(file) || service.canUse(file)) {
						dtde.acceptDrag(DnDConstants.ACTION_COPY);
						return;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		dtde.rejectDrag();
	}

	public void drop(DropTargetDropEvent dtde) {
		if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			dtde.rejectDrop();
			return;
		}
		dtde.acceptDrop(DnDConstants.ACTION_COPY);
		Transferable t = dtde.getTransferable();
		if (dtde.isLocalTransfer()) {
			localDrop(dtde);
			return;
		}
		try {
			List files = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
			Iterator f = files.iterator();
			while (f.hasNext()) {
				File file = (File) f.next();
				Iterator it = services.iterator();
				while (it.hasNext()) {
					ConversionService service = (ConversionService) it.next();
					if (service.canConvert(file) || service.canUse(file)) {
						Image image = service.getIcon(file);
						FileIcon icon = new FileIcon(file, image, service, false);
						getContentPane().add(icon);
						Point loc = dtde.getLocation();
						icon.setLocation(loc);
						scheduleConversion(icon);
						return;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startDrag(FileIcon src, DragGestureEvent dge) {
		Vector files = new Vector();
		Component[] components = getContentPane().getComponents();
		for (int i = 0; i < components.length; i++) {
			if (components[i] instanceof FileIcon) {
				FileIcon fi = (FileIcon) components[i];
				if (fi.highlighted)
					files.add(fi.file);
			}
		}
		Transferable transferable = new FileTransferable(files);
		InputEvent trigger = dge.getTriggerEvent();
		if (trigger instanceof MouseEvent) {
			dragStart = ((MouseEvent) trigger).getPoint();
			Point pos = src.getLocation();
			dragStart.x += pos.x;
			dragStart.y += pos.y;
		}
		dragSource.startDrag(dge, new Cursor(Cursor.MOVE_CURSOR), transferable, this);
	}

	public void localDrop(DropTargetDropEvent dtde) {
		Point dragEnd = dtde.getLocation();
		int dx = dragEnd.x - dragStart.x;
		int dy = dragEnd.y - dragStart.y;
		Component[] components = getContentPane().getComponents();
		for (int i = 0; i < components.length; i++) {
			if (components[i] instanceof FileIcon) {
				FileIcon fi = (FileIcon) components[i];
				if (fi.highlighted) {
					Point loc = fi.getLocation();
					loc.x += dx;
					loc.y += dy;
					fi.setLocation(loc);
				}
			}
		}

	}

	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	public void dragDropEnd(DragSourceDropEvent dsde) {
		// TODO Auto-generated method stub
	}

	public void dragEnter(DragSourceDragEvent dsde) {
		// TODO Auto-generated method stub
	}

	public void dragExit(DragSourceEvent dse) {
		// TODO Auto-generated method stub
	}

	public void dragOver(DragSourceDragEvent dsde) {
		// TODO Auto-generated method stub
	}

	public void dropActionChanged(DragSourceDragEvent dsde) {
		// TODO Auto-generated method stub
	}

	void scheduleConversion(FileIcon file) {
		synchronized (conversionQueue) {
			conversionQueue.add(file);
			conversionQueue.notify();
		}
	}

	public static void main(String[] args) {
		GUIDriver conv = new GUIDriver();
		conv.setVisible(true);
	}

}
