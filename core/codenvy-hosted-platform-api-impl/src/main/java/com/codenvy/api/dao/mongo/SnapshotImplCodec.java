/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.dao.mongo;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.eclipse.che.api.machine.server.impl.InstanceKeyImpl;
import org.eclipse.che.api.machine.server.impl.SnapshotImpl;

import java.util.List;

import static com.codenvy.api.dao.mongo.MongoUtil.documentsListAsMap;
import static com.codenvy.api.dao.mongo.MongoUtil.mapAsDocumentsList;

/**
 * Encodes & decodes {@link SnapshotImpl}.
 *
 * @author Sergii Kabashniuk
 */
public class SnapshotImplCodec implements Codec<SnapshotImpl> {

    private Codec<Document> codec;

    public SnapshotImplCodec(CodecRegistry registry) {
         codec = registry.get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, SnapshotImpl snapshot, EncoderContext encoderContext) {

        final Document document = new Document().append("_id", snapshot.getId())
                                                .append("workspaceId", snapshot.getWorkspaceId())
                                                .append("machineName", snapshot.getMachineName())
                                                .append("envName", snapshot.getEnvName())
                                                .append("type", snapshot.getType())
                                                .append("namespace", snapshot.getNamespace())
                                                .append("isDev", snapshot.isDev())
                                                .append("creationDate", snapshot.getCreationDate())
                                                .append("description", snapshot.getDescription())
                                                .append("instanceKey", mapAsDocumentsList(snapshot.getInstanceKey().getFields()));
        codec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<SnapshotImpl> getEncoderClass() {
        return SnapshotImpl.class;
    }

    @Override
    public SnapshotImpl decode(BsonReader reader, DecoderContext decoderContext) {
        final Document document = codec.decode(reader, decoderContext);
        return SnapshotImpl.builder()
                                         .setId(document.getString("_id"))
                                         .setWorkspaceId(document.getString("workspaceId"))
                                         .setMachineName(document.getString("machineName"))
                                         .setEnvName(document.getString("envName"))
                                         .setType(document.getString("type"))
                                         .setNamespace(document.getString("namespace"))
                                         .setDev(document.getBoolean("isDev"))
                                         .setCreationDate(document.getLong("creationDate"))
                                         .setDescription(document.getString("description"))
                                         .setInstanceKey(new InstanceKeyImpl(documentsListAsMap(
                                                 (List<Document>)document.get("instanceKey"))))
                                         .build();
    }
}
