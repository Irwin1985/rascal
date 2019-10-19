@license{
  Copyright (c) 2019 SWAT.engineering
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
}
@contributor{Paul Klint - Paul.Klint@swat.engineering - SWAT.engineering}

@doc{
.Synopsis
Library functions for source locations.

.Description

For a description of source locations see link:/Rascal#Values-Location[Location] in the Rascal Language Reference.

The following functions are defined for source locations:
loctoc::[1]
}
module Location

import IO;
import List;
import Exception;

@doc{
.Synopsis
Compare two location values lexicographically.

.Description
When the two locations refer to different files, their paths are compared as string.
When they refer to the same file, their offsets are compared when present.

.Pittfalls
This ordering regards the location value itself as opposed to the text it refers to.
}
bool isLexicallyLess(loc l1, loc l2)
    = l1.top == l2.top ? (l1.offset ? 0) < (l2.offset ? 0) : l1.top < l2.top;

@doc{
.Synopsis
Check that two locations refer to the same file.
}
bool sameFile(loc l1, loc l2)
    = l1.top == l2.top;

@doc{
.Synopsis
Get the textual content a location refers to.
}
str getContent(loc l)
    = readFile(l);

@doc{
.Synopsis
Is a location textually (strictly) contained in another location?
}

bool isStrictlyContainedIn(loc inner, loc outer)
    = inner.top == outer.top && ((inner.offset? && !outer.offset?) || inner.offset > outer.offset && inner.offset + inner.length < outer.offset + outer.length);

@doc{
.Synopsis
Is a location textually contained in another location?
}

bool isContainedIn(loc inner, loc outer)
    = inner.path == outer.path && ((inner.offset? && !outer.offset?) || inner.offset >= outer.offset && inner.offset + inner.length <= outer.offset + outer.length);


@doc{
.Synopsis
Refers a location to text that begins before (but may overlap with) the text referred to by another location?
}
bool beginsBefore(loc l, loc r)
    = l.path == r.path && l.offset < r.offset;
    
@doc{
.Synopsis
Refers a location to text completely before the text referred to by another location?
}
bool isBefore(loc l, loc r)
    = l.path == r.path && (l.offset <= r.offset && (l.offset + l.length) <= r.offset);

@doc{
.Synopsis
Refers a location to text _immediately_ before the text referred to by another location?
}
bool isImmediatelyBefore(loc l, loc r)
    = l.path == r.path && (l.offset <= r.offset && l.offset + l.length == r.offset);
 
 @doc{
.Synopsis
Refers a location to text that begins after (but may overlap with) the text referred to by another location?
}
bool beginsAfter(loc l, loc r)
    = l.path == r.path && l.offset > r.offset;
       
@doc{
.Synopsis
Refers a location to text completely after the text referred to by another location?
}
bool isAfter(loc l, loc r)
    = isBefore(r, l);

@doc{
.Synopsis
Refers a location to text _immediately_ after the text referred to by another location?
}
bool isImmediatelyAfter(loc l, loc r)
    = isImmediatelyBefore(r, l);

@doc{
.Synopsis
Refer two locations to text that overlaps?
}
bool isOverlapping(loc l, loc r)
    = l.path == r.path && ((l.offset <= r.offset && l.offset + l.length > r.offset) ||
                          (r.offset <= l.offset && r.offset + r.length > l.offset));

@doc{
.Synopsis
Take the union of a list of locations
.Description
Create a new location that refers to the smallest text area that overlaps with the given locations.
The given locations should all refer to the same file but they may be overlapping or be contained in each other.
}
loc union(list[loc] locs){
    switch(size(locs)){
    case 0: 
        throw IllegalArgument(locs, "Union of empty list of locations");
    case 1:
        return locs[0];
    default: {
            locs = [ l | l <- locs, !any(m <- locs, m != l, isContainedIn(l, m)) ];
            locs = sort(locs, beginsBefore);
            loc first = locs[0];
            loc last = locs[-1];
 
            scheme = first.scheme;
            authority = first.authority;
            path = first.path;
           
            for(l <- locs){
                if(l.scheme != scheme || l.authority != authority || l.path != path){
                    throw IllegalArgument(locs, "Union of locations with different scheme or path");
                }
            }
            return |<scheme>://<authority><path>|(first.offset, last.offset + last.length - first.offset, 
                                        <first.begin.line, first.begin.column>,
                                        <last.end.line, last.end.column>);
        }
    }
}
