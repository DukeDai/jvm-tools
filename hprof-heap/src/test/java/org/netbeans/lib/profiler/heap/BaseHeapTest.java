/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Alexey Ragozin
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.lib.profiler.heap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridkit.jvmtool.heapdump.HeapWalker.stringValue;
import static org.gridkit.jvmtool.heapdump.HeapWalker.walkFirst;

import java.util.ArrayList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.gridkit.jvmtool.heapdump.HeapWalker;
import org.junit.Test;

public abstract class BaseHeapTest {

    public abstract Heap getHeap();

    @Test
    public void verify_DummyA_via_classes() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyA.class.getName());
        int n = 0;
        for(Instance i : jclass.getInstances()) {
            ++n;
            assertThat(i.getFieldValues().size()).isEqualTo(1);
            FieldValue fv = i.getFieldValues().get(0);
            assertThat(fv).isInstanceOf(ObjectFieldValue.class);
            Instance ii = ((ObjectFieldValue)fv).getInstance();
            assertThat(ii).isInstanceOf(PrimitiveArrayInstance.class);
        }

        assertThat(n).isEqualTo(50);
    }

    @Test
    public void verify_DummyA_via_scan() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyA.class.getName());
        int n = 0;
        for(Instance i : heap.getAllInstances()) {
            if (i.getJavaClass() == jclass) {
                ++n;
                assertThat(i.getFieldValues().size()).isEqualTo(1);
                FieldValue fv = i.getFieldValues().get(0);
                assertThat(fv).isInstanceOf(ObjectFieldValue.class);
                Instance ii = ((ObjectFieldValue)fv).getInstance();
                assertThat(ii).isInstanceOf(PrimitiveArrayInstance.class);
            }
        }

        assertThat(n).isEqualTo(50);
    }

    @Test
    public void verify_heap_walker_for_dummyB() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyB.class.getName());
        int n = 0;
        for(Instance i : jclass.getInstances()) {
            ++n;
            int no = Integer.valueOf(stringValue(walkFirst(i, "seqNo")));
            SortedSet<String> testSet = new TreeSet<String>();
            for(Instance e :  HeapWalker.walk(i, "list.elementData[*]")) {
                if (e != null) {
                    testSet.add(stringValue(e));
                }
            }
            assertThat(testSet).isEqualTo(testSet("", no));

            testSet.clear();
            for(Instance e :  HeapWalker.walk(i, "map.table[*].key")) {
                if (e != null) {
                    testSet.add(stringValue(e));
                }
            }
            // some entries may be missing due to hash collisions
            assertThat(testSet("k", no).containsAll(testSet)).isTrue();

            testSet.clear();
            for(Instance e :  HeapWalker.walk(i, "map.table[*].value")) {
                if (e != null) {
                    testSet.add(stringValue(e));
                }
            }
            // some entries may be missing due to hash collisions
            assertThat(testSet("v", no).containsAll(testSet)).isTrue();
        }

        assertThat(n).isEqualTo(50);
    }

    @SuppressWarnings("unused")
    @Test
    public void verify_heap_walker_for_array_list() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(ArrayList.class.getName());
        int n = 0;
        for(Instance i : jclass.getInstances()) {
            int m = 0;
            for(Instance e :  HeapWalker.walk(i, "elementData[*](**.DummyA)")) {
                ++m;
            }
            if (m != 0) {
                ++n;
                assertThat(m).isEqualTo(50);
            }
        }

        assertThat(n).isEqualTo(1);
    }

    private Set<String> testSet(String pref, int limit) {
        Set<String> result = new TreeSet<String>();
        for(int i = 0; i != limit; ++i) {
            result.add(pref + i);
        }
        return result;
    }
}
