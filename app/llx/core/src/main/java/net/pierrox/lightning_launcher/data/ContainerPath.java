package net.pierrox.lightning_launcher.data;

import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;

public class ContainerPath {
//        0       page 0
//        0/2     page 0, container for item 2

        protected String mPath;

        public ContainerPath(String path) {
            mPath = path;
        }

        public ContainerPath(int page) {
            mPath = String.valueOf(page);
        }

        public ContainerPath(ItemView iv) {
            StringBuilder path = new StringBuilder();
            build(iv, path);
            mPath = path.toString();
        }

        public ContainerPath(ItemLayout il) {
            StringBuilder path = new StringBuilder();
            build(il, path);
            mPath = path.toString();
        }

        private void build(ItemView iv, StringBuilder out) {
            Item item = iv.getItem();
            ItemView openerItemView = iv.getParentItemLayout().getOpenerItemView();
            if(openerItemView == null) {
                out.append(item.getPage().id);
            } else {
                build(openerItemView, out);
            }
            out.append('/');
            out.append(item.getId());
        }

        private void build(ItemLayout il, StringBuilder out) {
            ItemView opener = il.getOpenerItemView();
            if(opener != null) {
                build(opener.getParentItemLayout(), out);
            } else {
                out.append(il.getPage().id);
            }
            if(opener != null) {
                out.append('/');
                out.append(opener.getItem().getId());
            }
        }

        public ContainerPath getParent() {
            int pos = mPath.lastIndexOf('/', mPath.length()-1);
            if(pos == -1) {
                return null;
            } else {
                return new ContainerPath(mPath.substring(0, pos));
            }
        }

        public int getLast() {
            return Integer.parseInt(mPath.substring(mPath.lastIndexOf('/')+1));
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ContainerPath && mPath.equals(((ContainerPath)o).mPath);
        }

        @Override
        public String toString() {
            return mPath;
        }
    }