/*
 * org.nrg.tip.restlet.JsonViews
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.restlet;

public class JsonViews {

    /**
     * Use this one if your "root" or "master" record is the patient. It will serialize the patient and its associated
     * studies, but, will not serialize the patient member of the Study object.
     *
     * @author ehaas01
     */
    public static class PatientRootView {
    }

    /**
     * Use this one if your "root" or "master" record is the study. It will serialize the study and its patient, but
     * will not serialize the studies member of the Patient object.
     *
     * @author ehaas01
     */
    public static class StudyRootView {
    }

    /**
     * No restrictions on this one currently - it will serialize all the way up the hierarchy.
     *
     * @author ehaas01
     */
    public static class SeriesRootView {
    }
}
