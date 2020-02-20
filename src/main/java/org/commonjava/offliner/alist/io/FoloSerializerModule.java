/*
 * Copyright (C) 2015 Red Hat, Inc. (jcasey@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.offliner.alist.io;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.StoreKeyDeserializer;
import org.commonjava.indy.model.core.io.StoreKeySerializer;

public class FoloSerializerModule
        extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    public static final FoloSerializerModule INSTANCE = new FoloSerializerModule();

    public FoloSerializerModule()
    {
        super( "AProx Core API" );
        addDeserializer( StoreKey.class, new StoreKeyDeserializer() );
        addSerializer( StoreKey.class, new StoreKeySerializer() );
    }

    @Override
    public int hashCode()
    {
        return getClass().getSimpleName()
                         .hashCode() + 17;
    }

    @Override
    public boolean equals( final Object other )
    {
        return getClass().equals( other.getClass() );
    }
}
