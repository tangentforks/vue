/*
 * Copyright 2003-2007 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import java.lang.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;

public class SelectionTool extends VueTool
{
    public SelectionTool() {
        super();
    }

    @Override
    public void handleToolSelection(boolean selected, VueTool otherTool) {
        super.handleToolSelection(selected, otherTool); // for debug
        if (selected) {
            if (otherTool instanceof ZoomTool) {
                ZoomTool zoomTool = (ZoomTool) otherTool;
                if (zoomTool.getZoomedTo() != null && zoomTool.getZoomedTo() instanceof LWSlide) {
                    final MapViewer viewer = VUE.getActiveViewer();
                    if (viewer == null)
                        return;
                    final LWSlide editingFocal = (LWSlide) zoomTool.getZoomedTo();
                    final LWComponent oldFocal = viewer.getFocal();
                    // not sure if we need oldFocal.
                    // anyway, this is a total hack that will be going away...
                    zoomTool.setEditingFocal(editingFocal, oldFocal);
                    viewer.loadFocal(editingFocal);
                }
            }
        }
    }    

    static class Direct extends SelectionTool {
        @Override
        public PickContext initPick(PickContext pc, float x, float y) {
            pc.pickDepth = 1;
            return pc;
        }
    }
    
}
