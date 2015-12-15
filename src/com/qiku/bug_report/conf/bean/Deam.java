package com.qiku.bug_report.conf.bean;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;

import com.qiku.bug_report.conf.bean.Deam.Tag.Type;

public class Deam extends ConfigEntry{
    //Dropbox Tag Name ---> namespace
    public Map<Tag.Key, String> mNamespaceDictionary;
    //Dropbox Tag Name ---> Tag Object
    public Map<Tag.Key, Tag> mTags;

    public Deam(){
        mNamespaceDictionary = new Hashtable<Tag.Key, String>();
        mTags = new Hashtable<Tag.Key, Tag>();
    }

    public Tag getTag(String name, Type type){
        //look for the tag with the name in mTags directly first
        Tag tag = mTags.get(new Tag.Key(name, type));
        if(tag == null){
            //If not found, look for it with it's name space
            String nameSpace = mNamespaceDictionary.get(new Tag.Key(name, type));
            if(!TextUtils.isEmpty(nameSpace))
                tag = mTags.get(new Tag.Key(nameSpace, type));
        }
        return tag;
    }

    public boolean hasTag(String name, Type type){
        return getTag(name, type) != null;
    }

    public static class Tag {
        public Key key;
        public List<Scenario> mScenarios;
        public Limit mLimit;
        //Use the combination of name and type as the key for a Tag
        public static class Key{
            public String mName;
            public Type mType;
            public Key(String name, Type type){
                this.mName = name;
                this.mType = type;
            }
            public String toString(){
                return mType + "-" + mName;
            }
            public int hashCode(){
                return (mType + "-" + mName).hashCode();
            }
            public boolean equals(Object anotherObject){
                if(anotherObject instanceof Key){
                    Key anotherKey = (Key)anotherObject;
                    return hashCode() == anotherKey.hashCode();
                }
                return false;
            }
        }

        public enum Type{
            USER,
            DROPBOX;
            public static Type toType(String name){
                try{
                    return Type.valueOf(name.toUpperCase());
                }catch(Exception e){
                    return Type.DROPBOX;
                }
            }
        }

        public static class Limit{
            public int mPeriod = 60; //period in seconds
            public int mMaxOccurrence = 10; //max occurrence allowed within the period
        }

        public Tag(){
            mScenarios = new ArrayList<Scenario>();
            mLimit = new Limit();
        }
    }
}
