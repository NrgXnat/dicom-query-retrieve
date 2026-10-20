/**
 * The studies the suite expects to find in the PACS.
 *
 * These are the "Disco" fixture studies loaded into the Orthanc instance on the
 * test stack. Global setup confirms every one is present before any test runs,
 * so a test failing on "study not found" means the fixture changed, not that
 * DQR is broken.
 *
 * The set has properties the tests rely on deliberately:
 *   - Studies 3 and 4 share an accession number.
 *   - Studies 1, 2 and 5 share a study date, and Study 3 is the day before.
 *   - Three modalities are present: MR, CT and US.
 *
 * Tests look studies up by these values and never by count, because export
 * tests add studies of their own to the PACS.
 */
export interface DiscoStudy {
    n: number;
    studyInstanceUid: string;
    accessionNumber: string;
    patientId: string;
    patientName: string;
    /** As DQR reports it on a study search: yyyy-MM-dd. */
    studyDate: string;
    modality: 'MR' | 'CT' | 'US';
}

const UID_ROOT = '1.3.6.1.4.1.14519.5.2.1.7777.8888.9999.';

function study(
    n: number,
    uid: string,
    accession: string,
    date: string,
    modality: DiscoStudy['modality'],
): DiscoStudy {
    return {
        n,
        studyInstanceUid: UID_ROOT + uid,
        accessionNumber: accession,
        patientId: `Disco Patient ${n}`,
        patientName: `Disco Patient ${n}`,
        studyDate: date,
        modality,
    };
}

export const DISCO = {
    1: study(1, '1697736458', '9999888800001', '2020-11-02', 'MR'),
    2: study(2, '1697737243', '9999888800002', '2020-11-02', 'CT'),
    3: study(3, '1697738325', '9999888800003', '2020-11-01', 'CT'),
    4: study(4, '1697739125', '9999888800003', '1999-12-13', 'CT'),
    5: study(5, '1697740368', '9999888800004', '2020-11-02', 'CT'),
    6: study(6, '1697741798', '9999888800005', '2017-12-06', 'MR'),
    7: study(7, '1697742621', '9999888800006', '1999-12-24', 'US'),
} as const;

export const ALL_DISCO: DiscoStudy[] = Object.values(DISCO);

/** The XNAT session data type each modality should become. */
export const SESSION_TYPE: Record<DiscoStudy['modality'], string> = {
    MR: 'xnat:mrSessionData',
    CT: 'xnat:ctSessionData',
    US: 'xnat:usSessionData',
};
