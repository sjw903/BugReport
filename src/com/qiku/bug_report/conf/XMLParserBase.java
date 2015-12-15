package com.qiku.bug_report.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.util.Log;

import com.qiku.bug_report.conf.bean.ConfigEntry;

public abstract class XMLParserBase<T extends ConfigEntry> {
    private static final String TAG = "BugReportXMLParserBase";
    public static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    public T parse(String filePath){
        if(filePath == null){
            throw new IllegalArgumentException("Invalid configuration file");
        }
        File file = new File(filePath);
        if(!file.exists()){
            throw new IllegalArgumentException("Invalid configuration file");
        }
        Log.d(TAG, "Parsing " + filePath);
        InputStream inputstream = null;
        try{
            inputstream = new FileInputStream(file);
            T entry = doParse(inputstream);
            if(entry != null)
                entry.mPath = filePath;
            return entry;
        }catch(Exception e){
            Log.e(TAG, "Error occured while parsing file " + file.getAbsolutePath(), e);
            return null;
        }finally{
            if(inputstream != null) try{ inputstream.close(); }catch(Exception e){}
        }
    }

    public T parse(InputStream inputstream){
        if(inputstream == null){
            throw new IllegalArgumentException("Invalid inputstream");
        }
        try{
            T entry = doParse(inputstream);
            return entry;
        }catch(Exception e){
            Log.e(TAG, "Error occured while parsing inputstream", e);
            return null;
        }finally{
            if(inputstream != null) try{ inputstream.close(); }catch(Exception e){}
        }
    }

    /**
     * subclass of XMLParserBase must implement this method to actually parse the XML file
     * @param inputstream
     * @return
     */
    protected abstract T doParse(InputStream inputstream);

    protected Element getRootElement(InputStream is)throws ParserConfigurationException, SAXException, IOException{
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        Element rootElement = doc.getDocumentElement();
        return rootElement;
    }

    protected String getNodeAttributeValueByName(Node node, String name){
        if(node == null || name == null)
            return null;

        NamedNodeMap nodeMap =  node.getAttributes();
        if(nodeMap == null)
            return null;

        Node attribute = nodeMap.getNamedItem(name);
        if(attribute == null)
            return null;

        return attribute.getTextContent();
    }

    protected Node getChildNodeByName(Node node, String name){
        if(node == null || name == null )
            return null;

        NodeList nodelist = node.getChildNodes();
        for(int i=0; i<nodelist.getLength(); i++){
            Node childNode = nodelist.item(i);
            if(childNode != null && childNode.getNodeName().equalsIgnoreCase(name))
                return childNode;
        }

        return null;
    }

    protected String getChildNodeValueByName(Node node, String name){
        Node childNode = getChildNodeByName(node, name);
        if(childNode != null)
            return childNode.getTextContent();
        return null;
    }
}
