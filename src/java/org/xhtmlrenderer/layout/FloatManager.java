/*
 * {{{ header & license
 * Copyright (c) 2005 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.layout;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.FloatedBlockBox;
import org.xhtmlrenderer.render.LineBox;
import org.xhtmlrenderer.render.RenderingContext;

public class FloatManager {
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    
    private List leftFloats = new ArrayList();
    private List rightFloats = new ArrayList();
    private Map offsetMap = new HashMap();
    
    private Box master;
    
    public void floatBox(LayoutContext c, BlockFormattingContext bfc, FloatedBlockBox box) {
        box.y = (int)c.getFloatingY();
        if (box.getStyle().isFloatedLeft()) {
            floatLeft(c, bfc, box);
        } else if (box.getStyle().isFloatedRight()) {
            floatRight(c, bfc, box);
        }
    }
    
    public void clear(CssContext cssCtx, BlockFormattingContext bfc, Box box) {
        if (box.getStyle().isClearLeft()) {
            moveClear(cssCtx, bfc, box, getFloats(LEFT));
        }
        if (box.getStyle().isClearRight()) {
            moveClear(cssCtx, bfc, box, getFloats(RIGHT));
        }
    }
    
    public void floatPending(LayoutContext c, BlockFormattingContext bfc, List pendingFloats) {
        if (pendingFloats.size() > 0) {
            for (int i = 0; i < pendingFloats.size(); i++) {
                if (i != 0) {
                    c.setFloatingY(0d);
                }
                FloatedBlockBox block = (FloatedBlockBox)pendingFloats.get(i);
                floatBox(c, bfc, block);
                block.setPending(false);
            }
            pendingFloats.clear();
        }
    }
    
    private void floatLeft(CssContext cssCtx, BlockFormattingContext bfc, 
            FloatedBlockBox current) {
        position(cssCtx, bfc, current, LEFT);
        save(current, bfc, LEFT);
    }
    
    private void floatRight(CssContext cssCtx, BlockFormattingContext bfc, 
            FloatedBlockBox current) {
        position(cssCtx, bfc, current, RIGHT);
        save(current, bfc, RIGHT);
    }    
    
    private void save(FloatedBlockBox current, BlockFormattingContext bfc, int direction) {
        offsetMap.put(current, bfc.getOffset());
        getFloats(direction).add(current);
    }
    
    private void position(CssContext cssCtx, BlockFormattingContext bfc, 
            FloatedBlockBox current, int direction) {
        moveAllTheWayOver(current, direction);
        
        
        alignToLastOpposingFloat(cssCtx, bfc, current, direction);
        alignToLastFloat(cssCtx, bfc, current, direction);
        
        if (! fitsInContainingBlock(current) || 
                overlaps(cssCtx, bfc, current, getFloats(direction))) {
            moveAllTheWayOver(current, direction);
            moveClear(cssCtx, bfc, current, getFloats(direction));
        }
        
        if (overlaps(cssCtx, bfc, current, getOpposingFloats(direction))) {
            moveAllTheWayOver(current, direction);
            moveClear(cssCtx, bfc, current, getFloats(direction));
            moveClear(cssCtx, bfc, current, getOpposingFloats(direction));
        }
        
        if (current.getStyle().isCleared()) {
            clear(cssCtx, bfc, current);
        }
    }
    
    private Point getOffset(Box box) {
        return (Point)offsetMap.get(box);
    }
    
    private List getFloats(int direction) {
        return direction == LEFT ? leftFloats : rightFloats;
    }
    
    private List getOpposingFloats(int direction) {
        return direction == LEFT ? rightFloats : leftFloats;
    }
    
    private void alignToLastFloat(CssContext cssCtx, 
            BlockFormattingContext bfc, FloatedBlockBox current, int direction) {
        
        List floats = getFloats(direction);
        if (floats.size() > 0) {
            Point offset = bfc.getOffset();
            FloatedBlockBox last = (FloatedBlockBox)floats.get(floats.size()-1);
            
            Rectangle currentBounds = current.getBounds(cssCtx, -offset.x, -offset.y);
            
            Point lastOffset = getOffset(last);
            Rectangle lastBounds = last.getBounds(cssCtx, -lastOffset.x, -lastOffset.y);
            
            boolean moveOver = false;
            
            if (currentBounds.y < lastBounds.y) {
                currentBounds.translate(0, lastBounds.y - currentBounds.y);
                moveOver = true;
            }
            
            if (currentBounds.y >= lastBounds.y && currentBounds.y < lastBounds.y + lastBounds.height) {
                moveOver = true;
            }
            
            if (moveOver) {
                if (direction == LEFT) {
                    currentBounds.x = lastBounds.x + last.getWidth();
                } else if (direction == RIGHT) {
                    currentBounds.x = lastBounds.x - current.getWidth();
                }
                
                currentBounds.translate(offset.x, offset.y);
                
                current.x = currentBounds.x;
                current.y = currentBounds.y;
            }
        }
    }
    
    private void alignToLastOpposingFloat(CssContext cssCtx, 
            BlockFormattingContext bfc, FloatedBlockBox current, int direction) {
        
        List floats = getOpposingFloats(direction);
        if (floats.size() > 0) {
            Point offset = bfc.getOffset();
            FloatedBlockBox last = (FloatedBlockBox)floats.get(floats.size()-1);
            
            Rectangle currentBounds = current.getBounds(cssCtx, -offset.x, -offset.y);
            
            Point lastOffset = getOffset(last);
            Rectangle lastBounds = last.getBounds(cssCtx, -lastOffset.x, -lastOffset.y);
            
            if (currentBounds.y < lastBounds.y) {
                currentBounds.translate(0, lastBounds.y - currentBounds.y);
                
                currentBounds.translate(offset.x, offset.y);
                
                current.y = currentBounds.y;
            }
        }
    }
    
    private void moveAllTheWayOver(FloatedBlockBox current, int direction) {
        if (direction == LEFT) {
            current.x = 0;
        } else if (direction == RIGHT) {
            current.x = current.getContainingBlock().contentWidth - current.getWidth();
        }
    }
    
    private boolean fitsInContainingBlock(FloatedBlockBox current) {
        return current.x >= 0 && 
            (current.x + current.getWidth()) <= current.getContainingBlock().contentWidth;
    }
    
    private int findLowestAbsoluteY(CssContext cssCtx, List floats) {
        int result = 0;
        
        for (Iterator i = floats.iterator(); i.hasNext(); ) {
            FloatedBlockBox floater = (FloatedBlockBox)i.next();
            
            Point offset = getOffset(floater);
            Rectangle bounds = floater.getBounds(cssCtx, -offset.x, -offset.y);
            if (bounds.y + bounds.height > result) {
                result = bounds.y + bounds.height;
            }
        }
        
        return result;
    }
    
    private boolean overlaps(CssContext cssCtx, BlockFormattingContext bfc, 
            FloatedBlockBox current, List floats) {
        Point offset = bfc.getOffset();
        Rectangle bounds = current.getBounds(cssCtx, -offset.x, -offset.y);
        
        for (Iterator i = floats.iterator(); i.hasNext(); ) {
            FloatedBlockBox floater = (FloatedBlockBox)i.next();
            Point floaterOffset = getOffset(floater);
            Rectangle floaterBounds = floater.getBounds(cssCtx, -floaterOffset.x, -floaterOffset.y);
            
            if (floaterBounds.intersects(bounds)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void moveClear(CssContext cssCtx, BlockFormattingContext bfc, 
            Box current, List floats) {
        Point offset = bfc.getOffset();
        Rectangle bounds = current.getBounds(cssCtx, -offset.x, -offset.y);
        
        int y = findLowestAbsoluteY(cssCtx, floats);
        
        if (bounds.y < y) {
            bounds.y = y;
            
            bounds.translate(offset.x, offset.y);
            
            current.y = bounds.y;
        }
    }
    
    public void paintFloats(RenderingContext c) {
        Box master = getMaster();
        
        c.translate(master.x, master.y);
        c.getGraphics().translate(master.x, master.y);
        
        for (Iterator i = leftFloats.iterator(); i.hasNext(); ) {
            Box floater = (Box)i.next();
            Point offset = (Point)offsetMap.get(floater);
            floater.paint(c, -offset.x, -offset.y);
        }
        
        for (Iterator i = rightFloats.iterator(); i.hasNext(); ) {
            Box floater = (Box)i.next();
            Point offset = (Point)offsetMap.get(floater);
            floater.paint(c, -offset.x, -offset.y);
        }
        
        c.getGraphics().translate(-master.x, -master.y);
        c.translate(-master.x, -master.y);
    }
    
    public void removeFloat(Box floater) {
        offsetMap.remove(floater);
        leftFloats.remove(floater);
        rightFloats.remove(floater);
    }
    
    private void applyLineWidthHeightHack(Box line, Rectangle bounds) {
        // this is a hack to deal with lines w/o width or height. is this valid?
        // possibly, since the line doesn't know how long it should be until it's already
        // done float adjustments
        if (line.contentWidth == 0) {
            bounds.width = 10;
        }
        if (line.height == 0) {
            bounds.height = 10;
        }
    }
    
    public int getLeftFloatDistance(CssContext cssCtx, BlockFormattingContext bfc, LineBox line) {
        return getFloatDistance(cssCtx, bfc, line, leftFloats, LEFT);
    }
    
    public int getRightFloatDistance(CssContext cssCtx, BlockFormattingContext bfc, LineBox line) {
        return getFloatDistance(cssCtx, bfc, line, rightFloats, RIGHT);
    }
    
    private int getFloatDistance(CssContext cssCtx, BlockFormattingContext bfc, 
            LineBox line, List floatsList, int direction) {
        if (floatsList.size() == 0) {
            return 0;
        }

        int xoff = 0;
        
        Point offset = bfc.getOffset();
        Rectangle lineBounds = line.getBounds(cssCtx, -offset.x, -offset.y);
        
        applyLineWidthHeightHack(line, lineBounds);

        for (int i = 0; i < floatsList.size(); i++) {
            // get the current float
            Box floater = (Box) floatsList.get(i);
            // create a rect from the box
            Point fpt = getOffset(floater);
            Rectangle fr = floater.getBounds(cssCtx, -fpt.x, -fpt.y);
            if (lineBounds.intersects(fr)) {
                lineBounds.translate(direction == LEFT ? fr.width : -fr.width, 0);
                xoff += fr.width;
            }
        }

        return xoff;
    }
    
    public void setMaster(Box owner) {
        this.master = owner;
    }
    
    public Box getMaster() {
        return master;
    }
}