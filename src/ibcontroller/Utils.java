// This file is part of the "IBController".
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2011 Richard L King (rlking@aultan.com)
// For conditions of distribution and use, see copyright notice in COPYING.txt

// IBController is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// IBController is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with IBController.  If not, see <http://www.gnu.org/licenses/>.

package ibcontroller;

import java.awt.Container;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

class Utils {
    
    static final SimpleDateFormat _DateFormatter = new SimpleDateFormat("HH:mm:ss:SSS");
    
    // set these to the defaults, so that we can continue to use them 
    // even when TWS redirects System.out and System.err to its own logfile
    private static final PrintStream out = System.out;
    private static final PrintStream err = System.err;
    
    private static boolean sendConsoleOutputToTwsLog = false;
    
    /**
     * Performs a click on the menu item at the specified path, waiting if necessary for the
     * menu item to become enabled.
     * 
     * If there is more than one menu bar within the specified container, they are searched
     * (in hierarchical containment order) until one is found that contains the specified menu item.
     * 
     * Note that this method may block the calling thread if the required menu item is currently disabled.
     * @param container
     * the Container to search in
     * @param path
     * the path of the required menu item
     * @return
     * true if the menu item was successfully clicked; false if the menu item could not be found
     * @throws  IllegalStateException 
     * the method has been called on the Swing event dispatch thread
     */
    static boolean invokeMenuItem(final Container container, final String[] path) throws IllegalStateException {
        if (SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Function must not be called on the event dispatch thread, as it may block the thread");
        while (true) {
            FutureTask<Boolean> task = new FutureTask<>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws IBControllerException {
                    String s = path[0];
                    for (int i = 1; i < path.length; i++) s = s + " > " + path[i];

                    JMenuItem menuItem = SwingUtils.findMenuItemInAnyMenuBar(container, path);
                    if (menuItem == null) throw new IBControllerException("menu item: " + s);
                    if (!menuItem.isEnabled()) return false;
                    menuItem.doClick();
                    return true;
                }
            });

            GuiDeferredExecutor.instance().execute(task);
            
            try {
                if (task.get()) return true;
            } catch (InterruptedException e) {
                logError("invokeMenuItem task interrupted");
                return false;
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                if (t instanceof IBControllerException) {
                    return false;
                }
                if (t instanceof RuntimeException) throw (RuntimeException)t;
                if (t instanceof Error) throw (Error)t;
            }
            
            pause(250);
        }
    }

    static void logError(String message) {
        getErrStream().println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        getErrStream().println(formatMessage(message));
        getErrStream().println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
    
    /**
     * Writes a plain one-line text message to the console.
     * @param msg
     * The message to be written
     */
    static void logRawToConsole(String msg) {
        getOutStream().println(msg);
    }
    
    /**
     * Writes a text message prefixed with the current time to the console.
     * @param msg
     * The message to be written
     */
    static void logToConsole(String msg) {
        getOutStream().println(formatMessage(msg));
    }
    
    private static PrintStream getErrStream() {
        if (sendConsoleOutputToTwsLog) {
            return System.err;
        } else {
            return err;
        }
    }
    
    private static PrintStream getOutStream() {
        if (sendConsoleOutputToTwsLog) {
            return System.out;
        } else {
            return out;
        }
    }
    
    private static String formatMessage(String message) {
        return _DateFormatter.format(new Date()) + " IBController: " + message;
    }

    /**
     * sleeps for millis milliseconds, approximately.
     * 
     * Note that this method swallows the InterruptedException that may
     * result from a call to sleep().
     * @param millis
     * the number of milliseconds to sleep for
     */
    static void pause(int millis) {
        try {
            Thread.sleep(millis); // sleep a bit before trying again.
        } catch (InterruptedException ie) {
        }
    }
    
    /**
     * Selects the specified section in the Global Configuration dialog.
     * @param configDialog
     * the Global Configuration dialog
     * @param path
     * the path to the required configuration section in the Global Configuration dialog
     * @return
     * true if the specified section can be found; otherwise false
     * @throws IBControllerException
     * a UI component could not be found
     * @throws IllegalStateException
     * the method has not been called on the SWing event dispatch thread
     */
    static boolean selectConfigSection(final JDialog configDialog, final String[] path) throws IBControllerException, IllegalStateException {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("selectConfigSection must be run on the event dispatch thread");
        
        JTree configTree = SwingUtils.findTree(configDialog);
        if (configTree == null) throw new IBControllerException("could not find the config tree in the Global Configuration dialog");

        Object node = configTree.getModel().getRoot();
        TreePath tp = new TreePath(node);

        for (String pathElement: path) {
            node = SwingUtils.findChildNode(configTree.getModel(), node, pathElement);
            if (node == null) return false;
            tp = tp.pathByAddingChild(node);
        }

        configTree.setExpandsSelectedPaths(true);
        configTree.setSelectionPath(tp);
        return true;
    }

    static void showTradesLogWindow() {
            MyCachedThreadPool.getInstance().execute(new Runnable () {
                @Override public void run() {invokeMenuItem(Environment.mainWindowManager().getMainWindow(), new String[] {"Account", "Trade Log"});}
            });
    }
    
    static void sendConsoleOutputToTwsLog(boolean value) {
        sendConsoleOutputToTwsLog = value;
    }

}

