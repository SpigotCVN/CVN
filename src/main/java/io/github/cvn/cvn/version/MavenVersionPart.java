package io.github.cvn.cvn.version;

interface MavenVersionPart extends Comparable<MavenVersionPart> {

    int getPrefixCodepoint();

    String stringifyContent();
}
