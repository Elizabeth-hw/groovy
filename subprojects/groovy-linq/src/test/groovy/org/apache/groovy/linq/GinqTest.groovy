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
package org.apache.groovy.linq


import groovy.transform.CompileStatic
import org.junit.Test

import static groovy.test.GroovyAssert.assertScript


@CompileStatic
class GinqTest {
    @Test
    void "testGinq - from select - 0"() {
        assertScript '''
            assert [0, 1, 2] == GINQ {
                from n in [0, 1, 2]
                select n
            }.toList()
        '''
    }

    @Test
    void "testGinq - from select - 1"() {
        assertScript '''
            def numbers = [0, 1, 2]
            assert [0, 1, 2] == GINQ {
                from n in numbers
                select n
            }.toList()
        '''
    }

    @Test
    void "testGinq - from select - 2"() {
        assertScript '''
            def numbers = [0, 1, 2]
            assert [0, 2, 4] == GINQ {
                from n in numbers
                select n * 2
            }.toList()
        '''
    }

    @Test
    void "testGinq - from select - 3"() {
        assertScript '''
            class Person {
                String name
                int age
                
                Person(String name, int age) {
                    this.name = name
                    this.age = age
                }
            }

            def persons = [new Person('Daniel', 35), new Person('Linda', 21), new Person('Peter', 30)]
            assert [35, 21, 30] == GINQ {
                from p in persons
                select p.age
            }.toList()
        '''
    }

    @Test
    void "testGinq - from select - 4"() {
        assertScript '''
            class Person {
                String name
                int age
                
                Person(String name, int age) {
                    this.name = name
                    this.age = age
                }
            }

            def persons = [new Person('Daniel', 35), new Person('Linda', 21), new Person('Peter', 30)]
            assert [['Daniel', 35], ['Linda', 21], ['Peter', 30]] == GINQ {
                from p in persons
                select p.name, p.age
            }.toList()
        '''
    }

    @Test
    void "testGinq - from select - 5"() {
        assertScript '''
            class Person {
                String name
                int age
                
                Person(String name, int age) {
                    this.name = name
                    this.age = age
                }
            }

            def persons = [new Person('Daniel', 35), new Person('Linda', 21), new Person('Peter', 30)]
            assert [[name:'Daniel', age:35], [name:'Linda', age:21], [name:'Peter', age:30]] == GINQ {
                from p in persons
                select (name: p.name, age: p.age)
            }.toList()
        '''
    }

    @Test
    void "testGinq - from select - 6"() {
        assertScript '''
            def numbers = [0, 1, 2]
            assert [0, 1, 2] == GINQ {
                from n in numbers select n
            }.toList()
        '''
    }

    @Test
    void "testGinq - from where select - 1"() {
        assertScript '''
            def numbers = [0, 1, 2, 3, 4, 5]
            assert [2, 4, 6] == GINQ {
                from n in numbers
                where n > 0 && n <= 3
                select n * 2
            }.toList()
        '''
    }

    @Test
    void "testGinq - from where select - 2"() {
        assertScript '''
            def numbers = [0, 1, 2, 3, 4, 5]
            assert [2, 4, 6] == GINQ {
                from n in numbers where n > 0 && n <= 3 select n * 2
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin select - 1"() {
        assertScript '''
            def nums1 = [1, 2, 3]
            def nums2 = [1, 2, 3]
            assert [[1, 1], [2, 2], [3, 3]] == GINQ {
                from n1 in nums1
                innerJoin n2 in nums2
                on n1 == n2
                select n1, n2
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin select - 2"() {
        assertScript '''
            def nums1 = [1, 2, 3]
            def nums2 = [1, 2, 3]
            assert [[2, 1], [3, 2], [4, 3]] == GINQ {
                from n1 in nums1
                innerJoin n2 in nums2
                on n1 == n2
                select n1 + 1, n2
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin select - 3"() {
        assertScript '''
            def nums1 = [1, 2, 3]
            def nums2 = [1, 2, 3]
            assert [[1, 2], [2, 3], [3, 4]] == GINQ {
                from n1 in nums1
                innerJoin n2 in nums2
                on n1 == n2
                select n1, n2 + 1
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin select - 4"() {
        assertScript '''
            def nums1 = [1, 2, 3]
            def nums2 = [1, 2, 3]
            assert [[1, 2], [2, 3]] == GINQ {
                from n1 in nums1
                innerJoin n2 in nums2
                on n1 + 1 == n2
                select n1, n2
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin select - 5"() {
        assertScript '''
            def nums1 = [1, 2, 3]
            def nums2 = [1, 2, 3]
            assert [[1, 2], [2, 3]] == GINQ {
                from n1 in nums1 innerJoin n2 in nums2 on n1 + 1 == n2 select n1, n2
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin select - 6"() {
        assertScript '''
            class Person {
                String name
                int age
                
                Person(String name, int age) {
                    this.name = name
                    this.age = age
                }
            }

            def persons1 = [new Person('Daniel', 35), new Person('Linda', 21), new Person('Peter', 30)]
            def persons2 = [new Person('Jack', 35), new Person('Rose', 21), new Person('Smith', 30)]
            assert [['Daniel', 'Jack'], ['Linda', 'Rose'], ['Peter', 'Smith']] == GINQ {
                from p1 in persons1
                innerJoin p2 in persons2
                on p1.age == p2.age
                select p1.name, p2.name
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin select - 7"() {
        assertScript '''
            class Person {
                String name
                int age
                
                Person(String name, int age) {
                    this.name = name
                    this.age = age
                }
            }
            
            def persons1 = [new Person('Daniel', 35), new Person('Linda', 21), new Person('Peter', 30)]
            def persons2 = [new Person('Jack', 35), new Person('Rose', 21), new Person('Smith', 30)]
            assert [['DANIEL', 'JACK'], ['LINDA', 'ROSE'], ['PETER', 'SMITH']] == GINQ {
                from p1 in persons1
                innerJoin p2 in persons2
                on p1.age == p2.age
                select p1.name.toUpperCase(), p2.name.toUpperCase()
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin select - 8"() {
        assertScript '''
            class Person {
                String name
                int age
                
                Person(String name, int age) {
                    this.name = name
                    this.age = age
                }
            }
            
            def same(str) { str }

            def persons1 = [new Person('Daniel', 35), new Person('Linda', 21), new Person('Peter', 30)]
            def persons2 = [new Person('Jack', 35), new Person('Rose', 21), new Person('Smith', 30)]
            assert [['DANIEL', 'JACK'], ['LINDA', 'ROSE'], ['PETER', 'SMITH']] == GINQ {
                from p1 in persons1
                innerJoin p2 in persons2
                on p1.age == p2.age
                select same(p1.name.toUpperCase()), same(p2.name.toUpperCase())
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin where select - 1"() {
        assertScript '''
            def nums1 = [1, 2, 3]
            def nums2 = [1, 2, 3]
            assert [[2, 2], [3, 3]] == GINQ {
                from n1 in nums1
                innerJoin n2 in nums2
                on n1 == n2
                where n1 > 1 && n2 <= 3
                select n1, n2
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin where select - 2"() {
        assertScript '''
            def nums1 = [1, 2, 3]
            def nums2 = [1, 2, 3]
            assert [[2, 2], [3, 3]] == GINQ {
                from n1 in nums1
                innerJoin n2 in nums2
                on n1 == n2
                where Math.pow(n1, 1) > 1 && Math.pow(n2, 1) <= 3
                select n1, n2
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin where select - 3"() {
        assertScript '''
            def nums1 = [1, 2, 3]
            def nums2 = [1, 2, 3]
            assert [[2, 2], [3, 3]] == GINQ {
                from n1 in nums1 innerJoin n2 in nums2 on n1 == n2 where Math.pow(n1, 1) > 1 && Math.pow(n2, 1) <= 3 select n1, n2
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin where select - 4"() {
        assertScript '''
            class Person {
                String name
                int age
                
                Person(String name, int age) {
                    this.name = name
                    this.age = age
                }
            }

            def persons1 = [new Person('Daniel', 35), new Person('Linda', 21), new Person('David', 30)]
            def persons2 = [new Person('Jack', 35), new Person('Rose', 21), new Person('Smith', 30)]
            assert [['Daniel', 'Jack']] == GINQ {
                from p1 in persons1
                innerJoin p2 in persons2
                on p1.age == p2.age
                where p1.name.startsWith('D') && p2.name.endsWith('k')
                select p1.name, p2.name
            }.toList()
        '''
    }

    @Test
    void "testGinq - from innerJoin where select - 5"() {
        assertScript '''
            class Person {
                String name
                int age
                
                Person(String name, int age) {
                    this.name = name
                    this.age = age
                }
            }
            
            def same(obj) {obj}

            def persons1 = [new Person('Daniel', 35), new Person('Linda', 21), new Person('David', 30)]
            def persons2 = [new Person('Jack', 35), new Person('Rose', 21), new Person('Smith', 30)]
            assert [['Daniel', 'Jack']] == GINQ {
                from p1 in persons1
                innerJoin p2 in persons2
                on p1.age == p2.age
                where same(p1.name.startsWith('D')) && same(p2.name.endsWith('k'))
                select p1.name, p2.name
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 0"() {
        assertScript '''
            assert [1, 2, 3] == GINQ {
                from v in (
                    from n in [1, 2, 3]
                    select n
                )
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 1"() {
        assertScript '''
            def numbers = [1, 2, 3]
            assert [1, 2, 3] == GINQ {
                from v in (
                    from n in numbers
                    select n
                )
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 2"() {
        assertScript '''
            def numbers = [1, 2, 3]
            assert [1, 2] == GINQ {
                from v in (
                    from n in numbers
                    where n < 3
                    select n
                )
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 3"() {
        assertScript '''
            def numbers = [1, 2, 3]
            assert [2] == GINQ {
                from v in (
                    from n in numbers
                    where n < 3
                    select n
                )
                where v > 1
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 4"() {
        assertScript '''
            def nums1 = [1, 2, 3, 4, 5]
            def nums2 = [1, 2, 3, 4, 5]
            assert [[3, 3], [5, 5]] == GINQ {
                from v in (
                    from n1 in nums1
                    innerJoin n2 in nums2
                    on n1 == n2
                    where n1 > 1 && n2 <= 5
                    select n1, n2
                )
                where v.n1 >= 3 && v.n2 in [3, 5]
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 5"() {
        assertScript '''
            def nums1 = [1, 2, 3, 4, 5]
            def nums2 = [1, 2, 3, 4, 5]
            assert [[3, 3], [5, 5]] == GINQ {
                from v in (
                    from n1 in nums1
                    innerJoin n2 in nums2
                    on n1 == n2
                    where n1 > 1 && n2 <= 5
                    select n1, n2
                )
                where v['n1'] >= 3 && v['n2'] in [3, 5]
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 6"() {
        assertScript '''
            def nums1 = [1, 2, 3, 4, 5]
            def nums2 = [1, 2, 3, 4, 5]
            assert [[3, 3], [5, 5]] == GINQ {
                from v in (
                    from n1 in nums1
                    innerJoin n2 in nums2
                    on n1 == n2
                    where n1 > 1 && n2 <= 5
                    select n1, n2
                )
                where v[0] >= 3 && v[1] in [3, 5] // v[0] references column1 n1, and v[1] references column2 n2
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 7"() {
        assertScript '''
            def nums1 = [1, 2, 3, 4, 5]
            def nums2 = [1, 2, 3, 4, 5]
            assert [[3, 3], [5, 5]] == GINQ {
                from v in (
                    from n1 in nums1
                    innerJoin n2 in nums2
                    on n1 == n2
                    where n1 > 1 && n2 <= 5
                    select n1 as vn1, n2 as vn2 // rename column names
                )
                where v.vn1 >= 3 && v.vn2 in [3, 5]
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 8"() {
        assertScript '''
            def nums1 = [1, 2, 3, 4, 5]
            def nums2 = [1, 2, 3, 4, 5]
            assert [[3, 3], [5, 5]] == GINQ {
                from v in (
                    from n1 in nums1
                    innerJoin n2 in nums2
                    on n1 == n2
                    where n1 > 1 && n2 <= 5
                    select ((n1 as Integer) as vn1), ((n2 as Integer) as vn2)
                )
                where v.vn1 >= 3 && v.vn2 in [3, 5]
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 9"() {
        assertScript '''
            assert [2, 6] == GINQ {
                from v in (
                    from n in (
                        from m in [1, 2, 3]
                        select m as v1, (m + 1) as v2
                    )
                    where n.v2 < 4
                    select n.v1 * n.v2
                )
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 10"() {
        assertScript '''
            assert [2, 6] == GINQ {
                from v in (
                    from n in (
                        from m in [1, 2, 3]
                        select m, (m + 1) as v2
                    )
                    where n.v2 < 4
                    select n.m * n.v2
                )
                select v
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 11"() {
        assertScript '''
            assert [[1, 2], [2, 3]] == GINQ {
                from v in (
                    from n in (
                        from m in [1, 2, 3]
                        select m, (m + 1) as v2
                    )
                    where n.v2 < 4
                    select n.m, n.v2   // its column names are: m, v2
                )
                select v.m, v.v2
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 12"() {
        assertScript '''
            assert [[1, 2], [2, 3]] == GINQ {
                from v in (
                    from n in (
                        from m in [1, 2, 3]
                        select m, (m + 1) as v2
                    )
                    where n.v2 < 4
                    select n.m, n.v2
                )
                select v."${'m'}", v.v2   // dynamic column name
            }.toList()
        '''
    }

    @Test
    void "testGinq - nested from - 13"() {
        assertScript '''
            assert [2, 6] == GINQ {
                from v in (
                    from n in (
                        from m in [1, 2, 3]
                        select m as v1, (m + 1) as v2
                    )
                    innerJoin k in [2, 3, 4]
                    on n.v2 == k
                    where n.v2 < 4
                    select n.v1 * k
                )
                select v
            }.toList()
        '''
    }
}
