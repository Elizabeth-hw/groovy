/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.groovy.linq.provider.collection

import groovy.transform.CompileStatic

/**
 * Immutable named list to represent list result of GINQ
 *
 * @since 4.0.0
 */
@CompileStatic
class NamedList<E> extends AbstractList<E> {
    private final List<E> elementList
    private final List<String> nameList

    private NamedList(List<E> elementList, List<String> nameList) {
        this.elementList = elementList
        this.nameList = nameList
    }

    E getAt(String name) {
        final int index = nameList.indexOf(name)

        if (-1 == index) {
            throw new IndexOutOfBoundsException("Failed to find element with name: $name")
        }

        return get(index)
    }

    E get(String name) {
        return getAt(name)
    }

    @Override
    E get(int index) {
        return elementList.get(index)
    }

    @Override
    int size() {
        return elementList.size()
    }
}
