/*
 *  Copyright (C) 2021 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fizzgate.stats;

/**
 *
 * @author Francis Dong
 *
 */
public enum BlockType {
	/**
	 * Blocked by concurrent request rule
	 */
	CONCURRENT_REQUEST,

	/**
	 * Blocked by QPS
	 */
	QPS,

	/**
	 * Blocked by degrade
	 */
	DEGRADE,

	CIRCUIT_BREAK
}
