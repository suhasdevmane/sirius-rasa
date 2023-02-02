/***********************************************************************************************
 * Copyright (c) 2022 Obeo. All Rights Reserved.
 * This software and the attached documentation are the exclusive ownership
 * of its authors and was conceded to the profit of Obeo S.A.S.
 * This software and the attached documentation are protected under the rights
 * of intellectual ownership, including the section "Titre II  Droits des auteurs (Articles L121-1 L123-12)"
 * By installing this software, you acknowledge being aware of these rights and
 * accept them, and as a consequence you must:
 * - be in possession of a valid license of use conceded by Obeo only.
 * - agree that you have read, understood, and will comply with the license terms and conditions.
 * - agree not to do anything that could conflict with intellectual ownership owned by Obeo or its beneficiaries
 * or the authors of this software.
 *
 * Should you not agree with these terms, you must stop to use this software and give it back to its legitimate owner.
 ***********************************************************************************************/
package org.eclipse.sirius.web.services.explorer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.sirius.components.collaborative.trees.api.ITreePathProvider;
import org.eclipse.sirius.components.collaborative.trees.dto.TreePath;
import org.eclipse.sirius.components.collaborative.trees.dto.TreePathInput;
import org.eclipse.sirius.components.collaborative.trees.dto.TreePathSuccessPayload;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IObjectService;
import org.eclipse.sirius.components.core.api.IPayload;
import org.eclipse.sirius.components.trees.Tree;
import org.eclipse.sirius.web.services.api.representations.IRepresentationService;
import org.eclipse.sirius.web.services.api.representations.RepresentationDescriptor;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ITreePathProvider} for the Sirius Web Explorer.
 *
 * @author pcdavid
 */
@Service
public class ExplorerTreePathProvider implements ITreePathProvider {

    private final IObjectService objectService;

    private final IRepresentationService representationService;

    public ExplorerTreePathProvider(IObjectService objectService, IRepresentationService representationService) {
        this.objectService = Objects.requireNonNull(objectService);
        this.representationService = Objects.requireNonNull(representationService);
    }

    @Override
    public boolean canHandle(Tree tree) {
        return tree != null && Objects.equals(ExplorerDescriptionProvider.DESCRIPTION_ID, tree.getDescriptionId());
    }

    @Override
    public IPayload handle(IEditingContext editingContext, Tree tree, TreePathInput input) {
        int maxDepth = 0;
        Set<String> allAncestors = new HashSet<>();
        for (String selectionEntryId : input.getSelectionEntryIds()) {
            List<String> itemAncestors = this.getAncestors(editingContext, selectionEntryId);
            allAncestors.addAll(itemAncestors);
            maxDepth = Math.max(maxDepth, itemAncestors.size());
        }
        return new TreePathSuccessPayload(input.getId(), new TreePath(allAncestors.stream().collect(Collectors.toList()), maxDepth));
    }

    private List<String> getAncestors(IEditingContext editingContext, String selectionEntryId) {
        List<String> ancestorsIds = new ArrayList<>();

        var optionalRepresentation = this.representationService.getRepresentation(UUID.fromString(selectionEntryId));
        var optionalSemanticObject = this.objectService.getObject(editingContext, selectionEntryId);

        Optional<Object> optionalObject = Optional.empty();
        if (optionalRepresentation.isPresent()) {
            // The first parent of a representation item is the item for its targetObject.
            // @formatter:off
            optionalObject = optionalRepresentation.map(RepresentationDescriptor::getTargetObjectId)
                    .flatMap(objectId -> this.objectService.getObject(editingContext, objectId));
            // @formatter:on
        } else if (optionalSemanticObject.isPresent()) {
            // The first parent of a semantic object item is the item for its actual container
            // @formatter:off
            optionalObject = optionalSemanticObject.filter(EObject.class::isInstance)
                    .map(EObject.class::cast)
                    .map(eObject -> Optional.<Object> ofNullable(eObject.eContainer()).orElse(eObject.eResource()));
            // @formatter:on
        }

        while (optionalObject.isPresent()) {
            ancestorsIds.add(this.getItemId(optionalObject.get()));
            // @formatter:off
            optionalObject = optionalObject
                    .filter(EObject.class::isInstance)
                    .map(EObject.class::cast)
                    .map(eObject -> Optional.<Object>ofNullable(eObject.eContainer()).orElse(eObject.eResource()));
            // @formatter:on
        }
        return ancestorsIds;

    }

    private String getItemId(Object object) {
        String result = null;
        if (object instanceof Resource) {
            Resource resource = (Resource) object;
            result = resource.getURI().toString();
        } else if (object instanceof EObject) {
            result = this.objectService.getId(object);
        }
        return result;
    }
}
