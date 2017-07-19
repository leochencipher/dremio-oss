/*
 * Copyright (C) 2017 Dremio Corporation
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
package com.dremio.dac.model.namespace;

import static com.dremio.service.namespace.proto.NameSpaceContainer.Type.SOURCE;

import java.util.ArrayList;
import java.util.List;

import com.dremio.dac.explore.model.Dataset;
import com.dremio.dac.explore.model.DatasetName;
import com.dremio.dac.explore.model.DatasetPath;
import com.dremio.dac.explore.model.DatasetResourcePath;
import com.dremio.dac.explore.model.DatasetVersionResourcePath;
import com.dremio.dac.model.common.DACRuntimeException;
import com.dremio.dac.model.common.NamespacePath;
import com.dremio.dac.model.folder.Folder;
import com.dremio.dac.model.folder.FolderPath;
import com.dremio.dac.model.folder.SourceFolderPath;
import com.dremio.dac.model.sources.PhysicalDataset;
import com.dremio.dac.model.sources.PhysicalDatasetName;
import com.dremio.dac.model.sources.PhysicalDatasetPath;
import com.dremio.dac.model.sources.PhysicalDatasetResourcePath;
import com.dremio.dac.model.sources.SourceName;
import com.dremio.dac.proto.model.dataset.VirtualDatasetUI;
import com.dremio.dac.service.datasets.DatasetVersionMutator;
import com.dremio.dac.service.errors.DatasetNotFoundException;
import com.dremio.dac.util.DatasetsUtil;
import com.dremio.file.File;
import com.dremio.file.FilePath;
import com.dremio.service.namespace.NamespaceException;
import com.dremio.service.namespace.NamespaceKey;
import com.dremio.service.namespace.NamespaceNotFoundException;
import com.dremio.service.namespace.NamespaceService;
import com.dremio.service.namespace.dataset.proto.DatasetConfig;
import com.dremio.service.namespace.dataset.proto.DatasetType;
import com.dremio.service.namespace.file.FileFormat;
import com.dremio.service.namespace.file.proto.FileType;
import com.dremio.service.namespace.physicaldataset.proto.PhysicalDatasetConfig;
import com.dremio.service.namespace.proto.NameSpaceContainer;
import com.dremio.service.namespace.proto.NameSpaceContainer.Type;
import com.dremio.service.namespace.space.proto.ExtendedConfig;
import com.dremio.service.namespace.space.proto.FolderConfig;
import com.google.common.base.Preconditions;

/**
 * Full/Partial representation of a namespace.
 */
public class NamespaceTree {

  // TODO For now we only implement list (single level lookups)
  private final List<Folder> folders;
  private final List<Dataset> datasets;
  private final List<File> files;
  private final List<PhysicalDataset> physicalDatasets;

  public NamespaceTree() {
    folders = new ArrayList<>();
    datasets = new ArrayList<>();
    files = new ArrayList<>();
    physicalDatasets = new ArrayList<>();
  }

  // Spaces, home and sources are top level folders hence can never show in children.
  public static NamespaceTree newInstance(final DatasetVersionMutator datasetService, final NamespaceService namespaceService,
      List<NameSpaceContainer> children, Type rootEntityType) throws NamespaceException, DatasetNotFoundException {
    NamespaceTree result = new NamespaceTree();

    populateInstance(result, datasetService, namespaceService, children, rootEntityType);

    return result;
  }

  protected static void populateInstance(NamespaceTree tree, final DatasetVersionMutator datasetService,
      final NamespaceService namespaceService, List<NameSpaceContainer> children, Type rootEntityType)
          throws NamespaceException, NamespaceNotFoundException, DatasetNotFoundException {
    for (final NameSpaceContainer container: children) {
      switch (container.getType()) {
        case FOLDER: {
          final List<NamespaceKey> datasetPaths = namespaceService.getAllDatasets(new FolderPath(container.getFullPathList()).toNamespaceKey());
          final ExtendedConfig extendedConfig = new ExtendedConfig()
            .setDatasetCount((long) datasetPaths.size())
            .setJobCount(datasetService.getJobsCount(datasetPaths));
          if (rootEntityType == SOURCE) {
            tree.addFolder(new SourceFolderPath(container.getFullPathList()), container.getFolder().setExtendedConfig(extendedConfig), null, rootEntityType);
          } else {
            tree.addFolder(new FolderPath(container.getFullPathList()), container.getFolder().setExtendedConfig(extendedConfig), rootEntityType);
          }
        }
        break;

        case DATASET: {
          final DatasetPath datasetPath = new DatasetPath(container.getFullPathList());
          final DatasetConfig datasetConfig = container.getDataset();
          switch (datasetConfig.getType()) {
            case VIRTUAL_DATASET:
              Preconditions.checkArgument(rootEntityType != SOURCE);
              final VirtualDatasetUI vds = datasetService.get(datasetPath, datasetConfig.getVirtualDataset().getVersion());
              tree.addDataset(
                new DatasetResourcePath(datasetPath),
                new DatasetVersionResourcePath(datasetPath, vds.getVersion()),
                datasetPath.getDataset(),
                vds.getSql(),
                vds,
                datasetService.getJobsCount(datasetPath.toNamespaceKey()),
                datasetService.getDescendantsCount(datasetPath.toNamespaceKey()),
                rootEntityType
              );
              break;

            case PHYSICAL_DATASET_HOME_FILE:
              final String fileDSId = container.getDataset().getId().getId();
            final FileFormat fileFormat = FileFormat.getForFile(DatasetsUtil.toFileConfig(container.getDataset()));
            tree.addFile(
                fileDSId,
                new FilePath(container.getFullPathList()),
                fileFormat,
                datasetService.getJobsCount(datasetPath.toNamespaceKey()),
                datasetService.getDescendantsCount(datasetPath.toNamespaceKey()), false, true,
                fileFormat.getFileType() != FileType.UNKNOWN, datasetConfig.getType()
              );
              break;

            case PHYSICAL_DATASET_SOURCE_FILE:
            case PHYSICAL_DATASET_SOURCE_FOLDER:
            case PHYSICAL_DATASET:
              PhysicalDatasetPath path = new PhysicalDatasetPath(datasetConfig.getFullPathList());
              tree.addPhysicalDataset(new
                      PhysicalDatasetResourcePath(new SourceName(container.getFullPathList().get(0)), path),
                      new PhysicalDatasetName(path.getFileName().getName()),
                      DatasetsUtil.toPhysicalDatasetConfig(container.getDataset()),
                      datasetService.getJobsCount(datasetPath.toNamespaceKey()),
                      datasetService.getDescendantsCount(datasetPath.toNamespaceKey())
                  );
              break;

            default:
              throw new DACRuntimeException("Possible corruption found. Invalid types in namespace tree " + children);
          }
        }
        break;

        default:
          throw new DACRuntimeException("Possible corruption found. Invalid types in namespace tree " + container.getType());
      }
    }
  }

  public void addFolder(final Folder f) {
    folders.add(f);
  }

  public void addFolder(SourceFolderPath folderPath, FolderConfig folderConfig, FileFormat fileFormat, NameSpaceContainer.Type rootEntityType) throws NamespaceNotFoundException {
    Folder folder = Folder.newInstance(folderPath, folderConfig, fileFormat, null, false, false);
    addFolder(folder);
  }


  public void addFolder(FolderPath folderPath, FolderConfig folderConfig, NameSpaceContainer.Type rootEntityType) throws NamespaceNotFoundException {
    Folder folder = Folder.newInstance(folderPath, folderConfig, null, false, false);
    addFolder(folder);
  }

  public void addFile(final File f) {
    files.add(f);
  }

  protected void addFile(String id, NamespacePath filePath, FileFormat fileFormat, Integer jobCount, Integer descendants,
      boolean isStaged, boolean isHomeFile, boolean isQueryable, DatasetType datasetType) {
    final File file = File.newInstance(
        id,
        filePath,
        fileFormat,
        jobCount,
        descendants, isStaged, isHomeFile, isQueryable
      );
      addFile(file);
  }

  public void addDataset(final Dataset ds) {
    datasets.add(ds);
  }

  protected void addDataset(DatasetResourcePath resourcePath,
      DatasetVersionResourcePath versionedResourcePath,
      DatasetName datasetName,
      String sql,
      VirtualDatasetUI datasetConfig,
      int jobCount, int descendants, NameSpaceContainer.Type rootEntityType) throws NamespaceNotFoundException {
    Dataset dataset = Dataset.newInstance(resourcePath, versionedResourcePath, datasetName, sql, datasetConfig, jobCount, descendants);

    addDataset(dataset);
  }

  public void addPhysicalDataset(final PhysicalDataset rds) {
    physicalDatasets.add(rds);
  }

  protected void addPhysicalDataset(
      PhysicalDatasetResourcePath resourcePath,
      PhysicalDatasetName datasetName,
      PhysicalDatasetConfig datasetConfig,
      Integer jobCount,
      Integer descendants) throws NamespaceNotFoundException {

    PhysicalDataset physicalDataset = new PhysicalDataset(resourcePath, datasetName, datasetConfig, jobCount, descendants);

    addPhysicalDataset(physicalDataset);
  }

  public final List<Folder> getFolders() {
    return folders;
  }

  public final List<Dataset> getDatasets() {
    return datasets;
  }

  public List<PhysicalDataset> getPhysicalDatasets() {
    return physicalDatasets;
  }

  public final List<File> getFiles() {
    return files;
  }
}
