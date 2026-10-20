/**
 * Builds a minimal, valid DICOM file with fresh UIDs.
 *
 * Export tests send XNAT sessions to the PACS. Sending a session that came
 * from the PACS would put copies of the fixture studies back into it, so the
 * suite generates its own: one tiny CT image per scan, with study, series and
 * instance UIDs made up for the run, and patient and accession values that
 * say where they came from. Nothing it sends can collide with existing data.
 *
 * The file is Explicit VR Little Endian with a 2 by 2, 16-bit image: the
 * smallest thing XNAT archives as a real CT scan.
 */
import { randomInt } from 'crypto';

const EXPLICIT_LE = '1.2.840.10008.1.2.1';
const CT_IMAGE_STORAGE = '1.2.840.10008.5.1.4.1.1.2';

/** A UID under the "2.25" root, which allows any UID made from a random integer. */
export function newUid(): string {
    let n = '';
    for (let i = 0; i < 4; i++) n += String(randomInt(1e9)).padStart(9, '0');
    return `2.25.${BigInt(n).toString()}`;
}

function pad(value: string, vr: string): Buffer {
    let b = Buffer.from(value, 'latin1');
    if (b.length % 2) b = Buffer.concat([b, Buffer.from(vr === 'UI' ? [0] : [0x20])]);
    return b;
}

function element(group: number, el: number, vr: string, value: Buffer): Buffer {
    const longForm = ['OB', 'OW', 'SQ', 'UN', 'UT'].includes(vr);
    const head = Buffer.alloc(longForm ? 12 : 8);
    head.writeUInt16LE(group, 0);
    head.writeUInt16LE(el, 2);
    head.write(vr, 4, 'latin1');
    if (longForm) head.writeUInt32LE(value.length, 8);
    else head.writeUInt16LE(value.length, 6);
    return Buffer.concat([head, value]);
}

const str = (g: number, e: number, vr: string, v: string) => element(g, e, vr, pad(v, vr));
const us = (g: number, e: number, v: number) => {
    const b = Buffer.alloc(2);
    b.writeUInt16LE(v);
    return element(g, e, 'US', b);
};

export interface GeneratedStudy {
    studyInstanceUid: string;
    patientName: string;
    patientId: string;
    accessionNumber: string;
    series: Array<{ seriesInstanceUid: string; seriesNumber: number; file: Buffer }>;
}

/** One study with `seriesCount` single-image CT series. */
export function generateStudy(tag: string, seriesCount = 2): GeneratedStudy {
    const studyInstanceUid = newUid();
    const patientName = `E2E^${tag}`;
    const patientId = `E2E_${tag}`;
    const accessionNumber = `E2E${tag}`.slice(0, 16);
    const series = Array.from({ length: seriesCount }, (_, i) => {
        const seriesInstanceUid = newUid();
        const sopInstanceUid = newUid();
        const dataset = Buffer.concat([
            str(0x0008, 0x0016, 'UI', CT_IMAGE_STORAGE),
            str(0x0008, 0x0018, 'UI', sopInstanceUid),
            str(0x0008, 0x0020, 'DA', '20240101'),
            str(0x0008, 0x0050, 'SH', accessionNumber),
            str(0x0008, 0x0060, 'CS', 'CT'),
            str(0x0008, 0x103e, 'LO', `E2E series ${i + 1}`),
            str(0x0010, 0x0010, 'PN', patientName),
            str(0x0010, 0x0020, 'LO', patientId),
            str(0x0020, 0x000d, 'UI', studyInstanceUid),
            str(0x0020, 0x000e, 'UI', seriesInstanceUid),
            str(0x0020, 0x0010, 'SH', `E2E${tag}`.slice(0, 16)),
            str(0x0020, 0x0011, 'IS', String(i + 1)),
            str(0x0020, 0x0013, 'IS', '1'),
            us(0x0028, 0x0002, 1),
            str(0x0028, 0x0004, 'CS', 'MONOCHROME2'),
            us(0x0028, 0x0010, 2),
            us(0x0028, 0x0011, 2),
            us(0x0028, 0x0100, 16),
            us(0x0028, 0x0101, 12),
            us(0x0028, 0x0102, 11),
            us(0x0028, 0x0103, 0),
            element(0x7fe0, 0x0010, 'OW', Buffer.from([0, 1, 0, 2, 0, 3, 0, 4])),
        ]);
        const metaBody = Buffer.concat([
            element(0x0002, 0x0001, 'OB', Buffer.from([0, 1])),
            str(0x0002, 0x0002, 'UI', CT_IMAGE_STORAGE),
            str(0x0002, 0x0003, 'UI', sopInstanceUid),
            str(0x0002, 0x0010, 'UI', EXPLICIT_LE),
            str(0x0002, 0x0012, 'UI', '2.25.1'),
        ]);
        const groupLength = Buffer.alloc(4);
        groupLength.writeUInt32LE(metaBody.length);
        const file = Buffer.concat([
            Buffer.alloc(128),
            Buffer.from('DICM', 'latin1'),
            element(0x0002, 0x0000, 'UL', groupLength),
            metaBody,
            dataset,
        ]);
        return { seriesInstanceUid, seriesNumber: i + 1, file };
    });
    return { studyInstanceUid, patientName, patientId, accessionNumber, series };
}
