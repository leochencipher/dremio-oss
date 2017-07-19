/*
 * Copyright 2016 Dremio Corporation
 */
package com.dremio.exec.store.easy.arrow;

import static com.dremio.exec.store.easy.arrow.ArrowFormatPlugin.MAGIC_STRING;

import java.io.IOException;
import java.util.List;

import org.apache.arrow.vector.ValueVector;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import com.dremio.common.exceptions.UserException;
import com.dremio.exec.cache.VectorAccessibleSerializable;
import com.dremio.exec.expr.TypeHelper;
import com.dremio.exec.proto.ExecProtos.FragmentHandle;
import com.dremio.exec.record.BatchSchema.SelectionVectorMode;
import com.dremio.exec.record.VectorAccessible;
import com.dremio.exec.record.VectorWrapper;
import com.dremio.exec.record.WritableBatch;
import com.dremio.exec.store.EventBasedRecordWriter;
import com.dremio.exec.store.RecordWriter;
import com.dremio.exec.store.WritePartition;
import com.dremio.exec.store.dfs.FileSystemWrapper;
import com.dremio.exec.store.dfs.easy.EasyWriter;
import com.dremio.exec.store.easy.arrow.ArrowFileFormat.ArrowFileFooter;
import com.dremio.exec.store.easy.arrow.ArrowFileFormat.ArrowFileMetadata;
import com.dremio.exec.store.easy.arrow.ArrowFileFormat.ArrowRecordBatchSummary;
import com.dremio.sabot.exec.context.OperatorContext;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * {@link RecordWriter} implementation for Arrow format files.
 */
public class ArrowRecordWriter implements RecordWriter {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EventBasedRecordWriter.class);

  private final EasyWriter writerConfig;
  private final List<Path> listOfFilesCreated;
  private final ArrowFileFooter.Builder footerBuilder;

  private Path location;
  private String prefix;
  private String extension;
  private FileSystemWrapper fs;

  private int nextFileIndex = 0;

  private Path currentFile;
  private FSDataOutputStream currentFileOutputStream;
  private OutputEntryListener listener;
  private VectorAccessible incoming;

  private long recordCount;
  private String relativePath;

  public ArrowRecordWriter(OperatorContext context, final EasyWriter writerConfig, ArrowFormatPluginConfig formatConfig) {
    final FragmentHandle handle = context.getFragmentHandle();

    this.writerConfig = writerConfig;
    this.listOfFilesCreated = Lists.newArrayList();
    this.footerBuilder = ArrowFileFooter.newBuilder();
    this.location = new Path(writerConfig.getLocation());
    this.prefix = String.format("%d_%d", handle.getMajorFragmentId(), handle.getMinorFragmentId());
    this.extension = formatConfig.outputExtension;
  }

  @Override
  public void setup(VectorAccessible incoming, OutputEntryListener listener) throws IOException {
    Preconditions.checkArgument(incoming.getSchema().getSelectionVectorMode() == SelectionVectorMode.NONE, "SelectionVector remover is not supported.");

    this.incoming = incoming;
    this.listener = listener;
    this.fs = FileSystemWrapper.get(location, writerConfig.getFsConf());
    this.currentFile = fs.canonicalizePath(new Path(location, String.format("%s_%d.%s", prefix, nextFileIndex, extension)));
    this.relativePath = currentFile.getName();
    this.currentFileOutputStream = fs.create(currentFile);
    listOfFilesCreated.add(currentFile);

    // write magic word bytes
    currentFileOutputStream.write(MAGIC_STRING.getBytes());

    for(final VectorWrapper<? extends ValueVector> vw : incoming) {
      Preconditions.checkArgument(!vw.isHyper(), "Writing hyper vectors to arrow format is not supported.");
      footerBuilder.addField(TypeHelper.getMetadata(vw.getValueVector()));
    }

    nextFileIndex++;
    recordCount = 0;
  }

  @Override
  public void startPartition(WritePartition partition) {
    if(!partition.isSinglePartition()){
      throw UserException.dataWriteError().message("Arrow writer doesn't support data partitioning.").build(logger);
    }
  }

  @Override
  public int writeBatch(int offset, int length) throws IOException {
    if(offset != 0 || length != incoming.getRecordCount()){
      throw UserException.dataWriteError().message("You cannot partition data written in Arrow format.").build(logger);
    }
    final int recordCount = incoming.getRecordCount();
    final long startOffset = currentFileOutputStream.getPos();

    final WritableBatch writableBatch = WritableBatch.getBatchNoHVWrap(recordCount, incoming, false /* isSv2 */);
    final VectorAccessibleSerializable serializer = new VectorAccessibleSerializable(writableBatch, null/*allocator*/);

    serializer.writeToStream(currentFileOutputStream);

    final ArrowRecordBatchSummary summary =
        ArrowRecordBatchSummary
            .newBuilder()
            .setOffset(startOffset)
            .setRecordCount(recordCount)
            .build();

    footerBuilder.addBatch(summary);

    this.recordCount += recordCount;

    return recordCount;
  }

  @Override
  public void abort() throws IOException {
    closeCurrentFile();
    for(final Path file : listOfFilesCreated) {
      fs.delete(file, true);
    }
  }

  private void closeCurrentFile() throws IOException {
    if (currentFileOutputStream != null) {
      // Save the footer starting offset
      final long footerStartOffset = currentFileOutputStream.getPos();

      // write the footer
      ArrowFileFooter footer = footerBuilder.build();
      footer.writeDelimitedTo(currentFileOutputStream);

      // write the foot offset
      currentFileOutputStream.writeLong(footerStartOffset);

      // write magic word bytes
      currentFileOutputStream.write(MAGIC_STRING.getBytes());

      currentFileOutputStream.close();
      currentFileOutputStream = null;

      ArrowFileMetadata lastFileMetadata =
          ArrowFileMetadata.newBuilder()
              .setFooter(footer)
              .setRecordCount(recordCount)
              .setPath(relativePath)
              .build();

      listener.recordsWritten(recordCount, currentFile.toString(), lastFileMetadata.toByteArray(), null);
    }
  }

  @Override
  public void close() throws Exception {
    closeCurrentFile();
  }
}
