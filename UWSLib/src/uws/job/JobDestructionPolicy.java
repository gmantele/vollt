package uws.job;

/*
 * This file is part of UWSLibrary.
 *
 * UWSLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * UWSLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with UWSLibrary.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2017 - Astronomisches Rechen Institut (ARI)
 */

import uws.job.user.JobOwner;

/**
 * Behavior to apply when a job destruction is asked.
 *
 * <p>
 * 	This policy must be set individually on {@link JobList} instances.
 * 	Thus, the jobs lists of a UWS service may have a different behavior when a
 * 	job destruction is required.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.3 (09/2017)
 * @since 4.3
 *
 * @since {@link JobList#destroyJob(String)}
 * @since {@link JobList#destroyJob(String, JobOwner)}
 */
public enum JobDestructionPolicy{
	/**
	 * <p><i><b>Default behavior in UWS-1.0 (and in UWSLibrary).</b></i></p>
	 *
	 * <p>
	 * 	Jobs are ALWAYS immediately destroyed and removed from the
	 * 	{@link JobList}.
	 * </p>
	 */
	ALWAYS_DELETE,
	/**
	 * <p><i><b>
	 * 	Behavior described in UWS-1.1 about the ARCHIVED phase.
	 * </b></i></p>
	 *
	 * <p>
	 * 	Jobs are archived when the destruction date is reached.
	 * </p>
	 *
	 * <p>
	 * 	The archived jobs are then still in the {@link JobList}
	 * 	but in the {@link ExecutionPhase#ARCHIVED} phase. An archived job is
	 * 	stopped and all its resources (uploads, results and threads) are freed.
	 * </p>
	 * <p>
	 * 	If the destruction date is yet not reached, the job is merely destroyed
	 * 	and removed from the {@link JobList}.
	 * </p>
	 * <p><i>Note:
	 * 	Destroying an archived job will definitely destroy it. Thus, the real
	 * 	destruction of a job may be done in 2 steps while using this policy.
	 * </i></p>
	 */
	ARCHIVE_ON_DATE,
	/**
	 * <p><i><b>
	 * 	Alternative behavior proposed by UWSLibrary (but never described in any
	 * 	UWS standard document).
	 * </b></i></p>
	 *
	 * <p>
	 * 	Jobs are ALWAYS immediately archived when a destruction is required
	 * 	(either because the destruction is reached or because of an explicit
	 * 	 action of the UWS client).
	 * </p>
	 *
	 * <p>
	 * 	The archived jobs are then still in the {@link JobList} but in the
	 * 	{@link ExecutionPhase#ARCHIVED} phase. An archived job is stopped and
	 * 	all its resources (uploads, results and threads) are freed.
	 * </p>
	 * <p><i>Note:
	 * 	Destroying an archived job will definitely destroy it. Thus, the real
	 * 	destruction of a job must be done in 2 steps while using this policy.
	 * </i></p>
	 */
	ALWAYS_ARCHIVE;
}
