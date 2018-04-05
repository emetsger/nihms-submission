/*
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataconservancy.nihms.assembler;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface PackageMetadataBuilder {

    /**
     * Sets the {@link PackageStream.Metadata#name()} of the {@code PackageStream}, suitable for use as a filename.
     *
     * @param name the package name
     * @return this builder
     * @see PackageStream.Metadata#name()
     */
    PackageMetadataBuilder name(String name);

    /**
     * Sets the {@link PackageStream.Metadata#spec()} of the {@code PackageStream}.
     *
     * @param spec the package specification
     * @return this builder
     * @see PackageStream.Metadata#spec()
     */
    PackageMetadataBuilder spec(String spec);

    /**
     * Sets the {@link PackageStream.Metadata#mimeType()} of the {@code PackageStream}, as returned by
     * {@link PackageStream#open()}
     *
     * @param mimeType the package specification
     * @return this builder
     * @see PackageStream.Metadata#mimeType()
     */
    PackageMetadataBuilder mimeType(String mimeType);

    /**
     * Sets the {@link PackageStream.Metadata#sizeBytes()} of the {@code PackageStream}, as returned by
     * {@link PackageStream#open()}
     *
     * @param sizeBytes the size o
     * @return this builder
     * @see PackageStream.Metadata#sizeBytes()
     */
    PackageMetadataBuilder sizeBytes(long sizeBytes);

    /**
     * Sets the {@link PackageStream.Metadata#compressed()} flag of the {@code PackageStream}.
     *
     * @param compressed the package specification
     * @return this builder
     * @see PackageStream.Metadata#compressed()
     */
    PackageMetadataBuilder compressed(boolean compressed);

    /**
     * Sets the {@link PackageStream.Metadata#compression()} used by the {@code PackageStream}, as returned by
     * {@link PackageStream#open()}.
     *
     * @param compression the package specification
     * @return this builder
     * @see PackageStream.Metadata#compression()
     */
    PackageMetadataBuilder compression(PackageStream.COMPRESSION compression);

    /**
     * Sets the {@link PackageStream.Metadata#archived()} flag of the {@code PackageStream}.
     *
     * @param archived the package specification
     * @return this builder
     * @see PackageStream.Metadata#archived()
     */
    PackageMetadataBuilder archived(boolean archived);

    /**
     * Sets the {@link PackageStream.Metadata#archive()} format used by the {@code PackageStream}, as returned by
     * {@link PackageStream#open()}.
     *
     * @param archive the package specification
     * @return this builder
     * @see PackageStream.Metadata#archive()
     */
    PackageMetadataBuilder archive(PackageStream.ARCHIVE archive);

    /**
     * Adds a {@link PackageStream.Metadata#checksum()} of the {@code PackageStream}.  The first
     * checksum added will be considered the "primary" checksum, returned in response to {@link
     * PackageStream.Metadata#checksum()}.
     *
     * @param checksum the package specification
     * @return this builder
     * @see PackageStream.Metadata#checksum()
     */
    PackageMetadataBuilder checksum(PackageStream.Checksum checksum);

    /**
     * Builds the Metadata object from the state set on this builder.
     *
     * @return the Metadata object
     */
    PackageStream.Metadata build();
}
