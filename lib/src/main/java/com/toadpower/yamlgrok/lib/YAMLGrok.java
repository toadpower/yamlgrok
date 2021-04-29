package com.toadpower.yamlgrok.lib;

import org.apache.commons.io.FilenameUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

public class YAMLGrok {

    public class Thing {
        final String type;
        final String name;
        Thing(String type, String name) {
            this.type = type;
            this.name = name;
        }
        @Override
        public int hashCode() {
            return type.hashCode() + name.hashCode();
        }
        @Override
        public boolean equals(Object o) {
            if (o==null) return false;
            if (!(o instanceof Thing)) return false;
            Thing other = (Thing)o;
            return other.type.equals(type) && other.name.equals(name);
        }
        @Override
        public String toString() {
            return type+":"+name;
        }
    }

    public class Link {
        final Thing local;
        final Thing remote;
        Link(Thing local, Thing remote) {
            this.local = local;
            this.remote = remote;
        }
        @Override
        public int hashCode() {
            return local.hashCode() + remote.hashCode();
        }
        @Override
        public boolean equals(Object o) {
            if (o==null) return false;
            if (!(o instanceof Link)) return false;
            Link other = (Link) o;
            return other.local.equals(local) && other.remote.equals(remote);
        }
        @Override
        public String toString() {
            return local + "->" + remote;
        }
    }

    private final Map<String, File> context;

    public Set<Link> foundLinkSet = new HashSet();
    public Set<Link> needLinkSet = new HashSet();
    public Set<Link> missingLinkSet = new HashSet();

    public Set<Thing> foundThingSet = new HashSet();
    public Set<Thing> needThingSet = new HashSet();
    public Set<Thing> missingThingSet = new HashSet();

    private YAMLGrok(Map<String, File> context) {
        this.context = context;
    }

    public static YAMLGrok create(File root, Collection<File> domain) {
        Map<String, File> context = new HashMap<String, File>();
        String prefix = FilenameUtils.normalize(root.getAbsolutePath()+"/", true);
        for(File typeFile : domain) {
            String filename = FilenameUtils.normalize(typeFile.getAbsolutePath(), true);
            String typeName = FilenameUtils.removeExtension(filename.substring(prefix.length()));
            context.put(typeName, typeFile);
        }
        return new YAMLGrok(context);
    }

    public boolean grok() throws FileNotFoundException {
        for(Map.Entry<String, File> entry : context.entrySet()) {
            String typeName = entry.getKey();
            File typeFile = entry.getValue();
            grokType(typeName, typeFile);
        }
        missingThingSet.addAll(needThingSet);
        missingThingSet.removeAll(foundThingSet);

        missingLinkSet.addAll(needLinkSet);
        missingLinkSet.removeAll(foundLinkSet);


        return missingLinkSet.isEmpty();
    }

    private void grokType(String typeName, File typeFile) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream is = new FileInputStream(typeFile);
        Map<String, Object> contents = yaml.load(is);
        if (contents==null)
            return;

        for(Map.Entry<String, Object> thingEntry : contents.entrySet()) {
            String thingName = thingEntry.getKey();
            Thing thing = new Thing(typeName, thingName);
            foundThingSet.add(thing);
            Object data = thingEntry.getValue();
            if (data == null) {
                continue;
            }
            if (data instanceof Map) {
                Map<String, Object> thingAttributes = (Map<String, Object>) data;
                grokThing(thing, thingAttributes);
            } else {
                continue;
            }
        }
    }

    private void grokThing(Thing thing, Map<String, Object> thingAttributes) {
        for(Map.Entry<String, Object> node : thingAttributes.entrySet()) {
            String nodeName = node.getKey();
            Object nodeData = node.getValue();
            if (nodeName.endsWith("<->")) {
                if (nodeData instanceof String) {
                    String nodeThing = (String) nodeData;
                    Thing other = new Thing(nodeName.substring(0, nodeName.length()-3), nodeThing);
                    needThingSet.add(other);
                    foundTwoWayLink(thing, other);
                } else if (nodeData instanceof List) {
                    for(String nodeThing : (List<String>)nodeData) {
                        Thing other = new Thing(nodeName.substring(0, nodeName.length()-3), nodeThing);
                        needThingSet.add(other);
                        foundTwoWayLink(thing, other);
                    }
                }
            } else if (nodeName.endsWith("->")) {
                if (nodeData instanceof String) {
                    String nodeThing = (String) nodeData;
                    Thing other = new Thing(nodeName.substring(0, nodeName.length()-2), nodeThing);
                    needThingSet.add(other);
                    foundOneWayLink(thing, other);
                } else if (nodeData instanceof List) {
                    for(String nodeThing : (List<String>)nodeData) {
                        Thing other = new Thing(nodeName.substring(0, nodeName.length()-2), nodeThing);
                        needThingSet.add(other);
                        foundOneWayLink(thing, other);
                    }
                }
            } else {
                if (nodeData instanceof Map) {
                    grokThing(thing, (Map<String, Object>)nodeData);
                }
            }
        }
    }

    private void foundTwoWayLink(Thing thing, Thing other) {
        foundLinkSet.add(new Link(thing, other));
        needLinkSet.add(new Link(other, thing));
    }

    private void foundOneWayLink(Thing thing, Thing other) {
        foundLinkSet.add(new Link(thing, other));
    }

}
