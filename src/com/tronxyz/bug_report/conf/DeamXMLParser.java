package com.tronxyz.bug_report.conf;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.text.TextUtils;
import android.util.Log;

import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.conf.bean.Deam;
import com.tronxyz.bug_report.conf.bean.EntryAttach;
import com.tronxyz.bug_report.conf.bean.EntryVariableParser;
import com.tronxyz.bug_report.conf.bean.ExecAttach;
import com.tronxyz.bug_report.conf.bean.ExecVariableParser;
import com.tronxyz.bug_report.conf.bean.FileAttach;
import com.tronxyz.bug_report.conf.bean.FileVariableParser;
import com.tronxyz.bug_report.conf.bean.Scenario;
import com.tronxyz.bug_report.conf.bean.Deam.Tag;
import com.tronxyz.bug_report.conf.bean.Scenario.Actions;
import com.tronxyz.bug_report.conf.bean.Scenario.Filter;
import com.tronxyz.bug_report.conf.bean.Scenario.Parser;
import com.tronxyz.bug_report.conf.bean.Scenario.Actions.Attach;
import com.tronxyz.bug_report.conf.bean.Scenario.Actions.Attach.Size;
import com.tronxyz.bug_report.conf.bean.Scenario.Actions.Attach.Size.Priority;
import com.tronxyz.bug_report.conf.bean.Scenario.Filter.Entry;
import com.tronxyz.bug_report.conf.bean.Scenario.Filter.Exec;
import com.tronxyz.bug_report.conf.bean.Scenario.Parser.VariableParser;
/**
 * The deam xml structure is as follows:
<?xml version="1.0" encoding="GBK"?>
<deam version="0.1">
    <namespace_dictionary> <!-- 0-1 -->
        <tag type="dropbox|user" name="foo"> <!-- 1-N -->
            <namespace>system::panic</namespace> <!-- 1 -->
        </tag>
    </namespace_dictionary>
    <tag type="dropbox|user" name="system"> <!-- 0-N -->
        <limit period="" max_occurrence=""/> <!-- 0-N -->
        <scenario name="s1" show_notification="TRUE|FALSE"> <!-- 0-N -->
            <parsers> <!-- 0-1 -->
                <entry> <!-- 0-1 -->
                    <!--- Parse the contents of the entry-->
                    <regex></regex> <!-- 1 -->
                    <vars> <!-- 1 -->
                        <var></var> <!-- 1-N -->
                    </vars>
                </entry>
                <file> <!-- 0-N -->
                    <name></name> <!-- 1 the source file name-->
                    <regex></regex> <!-- 1 -->
                    <vars> <!-- 1 -->
                        <var></var> <!-- 1-N -->
                    </vars>
                </file>
                <exec program=""> <!-- 0-N -->
                    <args> <!-- 0-N -->
                        <arg></arg> <!-- 1-N -->
                    </args> <!-- 0-1 -->
                    <regex></regex> <!-- 1 -->
                    <vars> <!-- 1 -->
                        <var></var> <!-- 1-N -->
                    </vars>
                </exec>
            </parsers>
            <filters> <!-- 0-1 -->
                <entry>
                    <regex></regex> <!-- 0-N -->
                </entry> <!-- 0-N -->
                <exec program=""> <!-- 0-N -->
                    <args> <!-- 0-N -->
                        <arg></arg> <!-- 1-N -->
                    </args> <!-- 0-1 -->
                    <ret_val></ret_val> <!-- 1 -->
                </exec>
            </filters>
            <actions>
                <attach> <!-- 0-1 -->
                    <entry> <!-- 0-1 -->
                        <size priority="head|tail"></size> <!-- 0-1 -->
                        <regex> <!-- 0-1 -->
                        </regex>
                    </entry>
                    <file> <!-- 0-N -->
                        <name></name> <!-- 1 -->
                        <size priority="head|tail"></size> <!-- 0-1 -->
                        <regex> <!-- 0-1 -->
                        </regex>
                    </file>
                    <exec program="sh">
                        <!-- args can have text content or child elements 'arg' -->
                        <args>
                            <arg>-c</arg>
                            <arg>cd /sdcard/bugreport; for file in $(ls bugreport*); do ls $file; done</arg>
                        </args> <!-- 0-1 -->
                        <size priority="head|tail"></size> <!-- 0-1 -->
                        <regex> <!-- 0-1 -->
                        </regex>
                    </exec>
                </attach>
                <should_create_bug /> <!-- 0-1 -->
            </actions>
        </scenario>
    </tag>
</deam>
*/
public class DeamXMLParser extends XMLParserBase<Deam> {
    private final static String TAG = "BugReportDeamParser";
    public static final String NAMESPACE_SEPARATOR = "::";
    public static final String TAG_NAME_DEAM = "deam";
    public static final String TAG_NAME_NAMESPACE_DIC = "namespace_dictionary";
    public static final String TAG_NAME_TAG = "tag";
    public static final String TAG_NAME_LIMIT = "limit";
    public static final String TAG_NAME_SCENARIO = "scenario";
    public static final String TAG_NAME_PARSERS = "parsers";
    public static final String TAG_NAME_VARS = "vars";
    public static final String TAG_NAME_VAR = "var";
    public static final String TAG_NAME_FILTERS = "filters";
    public static final String TAG_NAME_ENTRY = "entry";
    public static final String TAG_NAME_REGEX = "regex";
    public static final String TAG_NAME_EXEC = "exec";
    public static final String TAG_NAME_CMD = "cmd";
    public static final String TAG_NAME_RETVAL = "ret_val";
    public static final String TAG_NAME_ACTIONS = "actions";
    public static final String TAG_NAME_ATTACH = "attach";
    public static final String TAG_NAME_SIZE = "size";
    public static final String TAG_NAME_NAME = "name";
    public static final String TAG_NAME_OPTIONS = "options";
    public static final String TAG_NAME_FILE = "file";
    public static final String TAG_NAME_ARGS = "args";
    public static final String TAG_NAME_ARG = "arg";
    public static final String TAG_NAME_OUTPUT = "output";
    public static final String TAG_NAME_TIMEOUT = "timeout";
    public static final String TAG_NAME_NAMESPACE = "namespace";
    public static final String ATTRIBUTE_TAG_NAME = "name";
    public static final String ATTRIBUTE_TAG_TYPE = "type";
    public static final String ATTRIBUTE_SIZE_PRIORITY = "priority";
    public static final String ATTRIBUTE_EXEC_PROGRAM = "program";
    public static final String ATTRIBUTE_LIMIT_PERIOD = "period";
    public static final String ATTRIBUTE_LIMIT_MAX = "max_occurrence";
    public static final String ATTRIBUTE_SHOW_NOTIFICATION = "show_notification";

    public Deam doParse(InputStream is){
        Deam deam = null;
        try {
            Element root = getRootElement(is);
            if(root != null && root.hasChildNodes()){
                deam = new Deam();

                NodeList childNodes = root.getChildNodes();

                for(int i=0; i<childNodes.getLength(); i++){
                    Node childNode = childNodes.item(i);
                    if(childNode.getNodeType() != Node.ELEMENT_NODE)
                        continue;

                    if(childNode.getNodeName().equalsIgnoreCase(TAG_NAME_NAMESPACE_DIC))
                        parseNamespaceDictionary(childNode, deam);

                    if(childNode.getNodeName().equalsIgnoreCase(TAG_NAME_TAG))
                        parseTag(childNode, deam);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing DEAM", e);
        }
        return deam;
    }

    private void parseNamespaceDictionary(Node namespaceDicNode, Deam deam){
        if(namespaceDicNode == null || !namespaceDicNode.hasChildNodes() || deam == null)
            return;

        NodeList tagNodes = namespaceDicNode.getChildNodes();
        for(int j=0; tagNodes!=null && j<tagNodes.getLength(); j++){

            Node tagNode = tagNodes.item(j);
            if(tagNode == null || tagNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            String tagName =  getNodeAttributeValueByName(tagNode, ATTRIBUTE_TAG_NAME);
            Node namespaceNode = getChildNodeByName(tagNode, TAG_NAME_NAMESPACE);
            if(namespaceNode == null)
                continue;

            String namespace = namespaceNode.getTextContent();
            if(tagName == null ||  tagName.isEmpty() || namespace == null || namespace.trim().isEmpty())
                continue;

            Deam.Tag.Type tagType = Deam.Tag.Type.toType(getNodeAttributeValueByName(tagNode, ATTRIBUTE_TAG_TYPE));
            deam.mNamespaceDictionary.put(new Tag.Key(tagName, tagType), namespace);
        }
    }

    private void parseTag(Node tagNode, Deam deam){
        if(tagNode == null || deam == null)
            return;

        String tagName = getNodeAttributeValueByName(tagNode, ATTRIBUTE_TAG_NAME);
        if(tagName == null || tagName.trim().isEmpty())
            return;

        Deam.Tag.Type tagType = Deam.Tag.Type.toType(getNodeAttributeValueByName(tagNode, ATTRIBUTE_TAG_TYPE));

        Tag tag = new Tag();
        tag.key = new Tag.Key(tagName, tagType);
        deam.mTags.put(tag.key, tag);

        NodeList childNodes = tagNode.getChildNodes();
        for(int i=0; childNodes!=null && i<childNodes.getLength(); i++){
            Node node = childNodes.item(i);
            if(node == null || node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if(node.getNodeName().equalsIgnoreCase(TAG_NAME_SCENARIO)){
                parseScenario(node, tag);
            }else if(node.getNodeName().equalsIgnoreCase(TAG_NAME_LIMIT)){
                String period = getNodeAttributeValueByName(node, ATTRIBUTE_LIMIT_PERIOD);
                if(!TextUtils.isEmpty(period)) {
                    tag.mLimit.mPeriod = Integer.parseInt(period.trim());
                    if (0 >= tag.mLimit.mPeriod)
                        throw new IllegalArgumentException("deam.tag.limit.period must be > 0");
                }
                String maxOccurrence = getNodeAttributeValueByName(node, ATTRIBUTE_LIMIT_MAX);
                if(!TextUtils.isEmpty(maxOccurrence)) {
                    tag.mLimit.mMaxOccurrence = Integer.parseInt(maxOccurrence.trim());
                    if (0 >= tag.mLimit.mMaxOccurrence)
                        throw new IllegalArgumentException(
                                "deam.tag.limit.max_occurrence must be > 0");
                }
            }
        }
    }

    private void parseScenario(Node scenarioNode, Tag tag){
        if(scenarioNode == null || tag == null || !scenarioNode.hasChildNodes())
            return;
        Scenario scenario = new Scenario();
        tag.mScenarios.add(scenario);

        String name = getNodeAttributeValueByName(scenarioNode, ATTRIBUTE_TAG_NAME);
        scenario.mName = name;

        String notify = getNodeAttributeValueByName(scenarioNode, ATTRIBUTE_SHOW_NOTIFICATION);
        if(!TextUtils.isEmpty(notify)){
            scenario.mShowNotification = notify.equalsIgnoreCase("true");
        }

        NodeList childNodes = scenarioNode.getChildNodes();
        for(int i=0; childNodes!=null && i<childNodes.getLength(); i++){
            Node node = childNodes.item(i);
            if(node == null || node.getNodeType() != Node.ELEMENT_NODE)
                continue;
            if(node.getNodeName().equalsIgnoreCase(TAG_NAME_PARSERS)){
                parseParsers(node, scenario);
            }else if(node.getNodeName().equalsIgnoreCase(TAG_NAME_FILTERS)){
                parseFilters(node, scenario);
            }else if(node.getNodeName().equalsIgnoreCase(TAG_NAME_ACTIONS)){
                parseActions(node, scenario);
            }
        }
    }

    private void parseParsers(Node parsersNode, Scenario scenario){
        if(parsersNode == null || scenario == null || !parsersNode.hasChildNodes())
            return;
        Parser parser = new Parser();
        scenario.mParser = parser;
        NodeList childNodes = parsersNode.getChildNodes();
        for(int i=0; childNodes!=null && i<childNodes.getLength(); i++){
            Node node = childNodes.item(i);
            if(node == null || node.getNodeType() != Node.ELEMENT_NODE)
                continue;
            if(node.getNodeName().equalsIgnoreCase(TAG_NAME_ENTRY)){
                EntryVariableParser entryVariableParser = new EntryVariableParser();
                entryVariableParser.setParent(scenario);
                //parsers.entry.vars and parsers.entry.regex
                parseVariableParsers(node, entryVariableParser);
                parser.mParsers.add(entryVariableParser);
            }else if(node.getNodeName().equalsIgnoreCase(TAG_NAME_FILE)){
                String filePath = getChildNodeValueByName(node, TAG_NAME_NAME);
                if(!TextUtils.isEmpty(filePath)){
                    FileVariableParser fileVariableParser = new FileVariableParser();
                    fileVariableParser.setParent(scenario);
                    fileVariableParser.mFilePath = filePath;
                    //parsers.file.vars and parsers.file.regex
                    parseVariableParsers(node, fileVariableParser);
                    parser.mParsers.add(fileVariableParser);
                }
            }else if(node.getNodeName().equalsIgnoreCase(TAG_NAME_EXEC)){
                String program = getNodeAttributeValueByName(node, ATTRIBUTE_EXEC_PROGRAM);
                if(program == null || program.isEmpty()){
                    continue;
                }
                ExecVariableParser execVariableParser = new ExecVariableParser(program);
                execVariableParser.setParent(scenario);
                //parsers.exec.args
                parseExecArgs( getChildNodeByName(node, TAG_NAME_ARGS), execVariableParser.mCmdLine);
                //parsers.exec.vars and parsers.exec.regex
                parseVariableParsers(node, execVariableParser);
                parser.mParsers.add(execVariableParser);
            }
        }
    }

    private void parseVariableParsers(Node parserNode, VariableParser parser){
        if(parserNode == null || parser == null || !parserNode.hasChildNodes())
            return;

        String regex = getChildNodeValueByName(parserNode, TAG_NAME_REGEX);
        if(!TextUtils.isEmpty(regex))
            parser.mPattern = Pattern.compile(regex);

        Node varsNode = getChildNodeByName(parserNode, TAG_NAME_VARS);
        if(varsNode == null)
            return;

        NodeList varNodes = varsNode.getChildNodes();
        for(int i=0; varNodes!=null && i<varNodes.getLength(); i++){
            Node varNode = varNodes.item(i);
            if(varNode == null || varNode.getNodeType() != Node.ELEMENT_NODE)
                continue;
            if(varNode.getNodeName().equalsIgnoreCase(TAG_NAME_VAR)){
                String varName = varNode.getTextContent();
                if(varName != null && !TextUtils.isEmpty(varName.trim())){
                    parser.mVarNames.add(varName.trim());
                }
            }
        }
    }

    private void parseActions(Node actionsNode, Scenario scenario){
        if(actionsNode == null || scenario == null || !actionsNode.hasChildNodes())
            return;
        Actions actions = new Actions();
        scenario.mActions = actions;
        actions.setParent(scenario);
        NodeList childNodes = actionsNode.getChildNodes();
        for(int i=0; childNodes!=null && i<childNodes.getLength(); i++){
            Node node = childNodes.item(i);
            if(node == null || node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if(node.getNodeName().equalsIgnoreCase(TAG_NAME_ATTACH)){
                parseAttach(node, actions);
            }
        }
    }

    private void parseAttach(Node attachNode, Actions actions){
        if(attachNode == null || actions == null || !attachNode.hasChildNodes())
            return;
        NodeList childNodes = attachNode.getChildNodes();
        for(int i=0; childNodes!=null && i<childNodes.getLength(); i++){
            Node node = childNodes.item(i);
            if(node == null || node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if(node.getNodeName().equalsIgnoreCase(TAG_NAME_ENTRY)){
                parseEntryAttach(node, actions);
            }else if(node.getNodeName().equalsIgnoreCase(TAG_NAME_FILE)){
                parseFileAttach(node, actions);
            }else if(node.getNodeName().equalsIgnoreCase(TAG_NAME_EXEC)){
                parseExecAttach(node, actions);
            }
        }
    }

    private void parseEntryAttach(Node entryNode, Actions actions){
        EntryAttach entryAttach = new EntryAttach();
        actions.attaches.add(entryAttach);
        entryAttach.setParent(actions);
        //parsing entry size
        parseAttachSize(entryNode, entryAttach);
        //parsing entry regular expression
        String regex = getChildNodeValueByName(entryNode, TAG_NAME_REGEX);
        if(regex != null && !regex.trim().isEmpty()){
            entryAttach.mRegex = regex;
        }
    }

    private void parseFileAttach(Node fileNode, Actions actions){
        FileAttach fileAttach = new FileAttach();
        actions.attaches.add(fileAttach);
        fileAttach.setParent(actions);
        //file name
        String name = getChildNodeValueByName(fileNode, TAG_NAME_NAME);
        if(name != null && !name.trim().isEmpty()){
            fileAttach.mName = name;
        }
        //file size
        parseAttachSize(fileNode, fileAttach);
        //parsing file regular expression
        String regex = getChildNodeValueByName(fileNode, TAG_NAME_REGEX);
        if(regex != null && !regex.trim().isEmpty())
            fileAttach.mRegex = regex;
    }

    private void parseExecAttach(Node execNode, Actions actions){

        String program = getNodeAttributeValueByName(execNode, ATTRIBUTE_EXEC_PROGRAM);
        if(program == null || program.isEmpty()){
            return;
        }

        ExecAttach execAttach = new ExecAttach(program);
        actions.attaches.add(execAttach);
        execAttach.setParent(actions);

        //args tag
        parseExecArgs(getChildNodeByName(execNode, TAG_NAME_ARGS), execAttach.mCmdLine);

        //timeout tag
        String timeout = getChildNodeValueByName(execNode, TAG_NAME_TIMEOUT);
        if(timeout != null && !timeout.trim().isEmpty() && timeout.trim().matches("\\d+"))
            execAttach.mTimeout = Integer.parseInt(timeout.trim());
        else
            execAttach.mTimeout = Constants.BUGREPORT_EXEC_TIMEOUT;

        //output file name of the command
        String output = getChildNodeValueByName(execNode, TAG_NAME_OUTPUT);
        if(output != null && !output.trim().isEmpty()){
            execAttach.mOutputFileName = output;
        }

        //command size
        parseAttachSize(execNode, execAttach);
        //regular expression
        String regex = getChildNodeValueByName(execNode, TAG_NAME_REGEX);
        if(regex != null && !regex.trim().isEmpty())
            execAttach.mRegex = regex;
    }

    private void parseAttachSize(Node attachNode, Attach attach){
        if(attachNode == null || attach == null)
            return;
        Node sizeNode = getChildNodeByName(attachNode, TAG_NAME_SIZE);
        if(sizeNode != null){
            Size size = new Size();
            attach.size = size;
            String priority = getNodeAttributeValueByName(sizeNode, ATTRIBUTE_SIZE_PRIORITY);
            if(priority!=null && !priority.isEmpty()){
                size.priority = Priority.toPriority(priority);
            }
            String sizeValue = sizeNode.getTextContent();
            if(sizeValue != null && sizeValue.matches("[1-9]{1}\\d*")){
                size.length = Long.parseLong(sizeValue);
            }
        }
    }

    private void parseExecArgs(Node argsNode, CommandLine cmdLine){
        if(argsNode == null || cmdLine == null)
            return;

        NodeList childNodes = argsNode.getChildNodes();
        //The args node has only a text type of child node
        if (childNodes.getLength() == 1 && childNodes.item(0).getNodeType()==Node.TEXT_NODE) {
            String args = argsNode.getTextContent();
            if(args != null && !TextUtils.isEmpty(args.trim())){
                // The user wants us to parse out the args.
                cmdLine.addArguments(args);
            }
        }else{
            // Either there are no args, or the user has specified them individually
            // with one or more <arg> tags.
            for(int i=0; childNodes!=null && i<childNodes.getLength(); i++){
                Node childNode = childNodes.item(i);
                if(childNode == null || childNode.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                if(childNode.getNodeName().equalsIgnoreCase(TAG_NAME_ARG)){
                    String arg = childNode.getTextContent();
                    if(arg != null && !arg.trim().isEmpty()){
                        //add the argument without handling quoting
                        cmdLine.addArgument(arg, false);
                    }
                }
            }
        }
    }

    private void parseFilters(Node filtersNode, Scenario scenario){
        if(filtersNode == null || scenario == null || !filtersNode.hasChildNodes())
            return;
        Filter filter = new Filter();
        scenario.mFilter = filter;
        NodeList childNodes = filtersNode.getChildNodes();
        for(int i=0; childNodes!=null && i<childNodes.getLength(); i++){
            Node node = childNodes.item(i);
            if(node == null || node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if(node.getNodeName().equalsIgnoreCase(TAG_NAME_ENTRY)){
                parseEntry(node, filter);
            }else if(node.getNodeName().equalsIgnoreCase(TAG_NAME_EXEC)){
                parseExec(node, filter);
            }
        }
    }

    private void parseEntry(Node entryNode, Filter filter){
        if(entryNode == null || filter == null || !entryNode.hasChildNodes())
            return;
        filter.entry = new Entry();
        NodeList childNodes = entryNode.getChildNodes();
        for(int i=0; childNodes!=null && i<childNodes.getLength(); i++){
            Node node = childNodes.item(i);
            if(node == null || node.getNodeType() != Node.ELEMENT_NODE)
                continue;
            if(node.getNodeName().equalsIgnoreCase(TAG_NAME_REGEX)){
                String regex = node.getTextContent();
                if(regex != null &&  !regex.isEmpty()){
                    filter.entry.regexs.add(regex);
                }
            }
        }
    }

    private void parseExec(Node execNode, Filter filter){
        if(execNode == null || filter == null || !execNode.hasChildNodes())
            return;
        String program = getNodeAttributeValueByName(execNode, ATTRIBUTE_EXEC_PROGRAM);
        if (program == null || program.isEmpty()) {
            return;
        }
        Exec exec = new Exec();
        exec.cmd = new CommandLine(program);
        filter.execs.add(exec);

        //args tag
        parseExecArgs(getChildNodeByName(execNode, TAG_NAME_ARGS), exec.cmd);

        //ret_val tag
        String ret_val = getChildNodeValueByName(execNode, TAG_NAME_RETVAL);
        if (ret_val != null && !ret_val.trim().isEmpty()) {
            exec.ret_val = ret_val;
        }
    }
}
