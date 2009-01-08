/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.xnio;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CancellationException;
import java.io.IOException;

/**
 * The future result of an asynchronous request.  Use instances of this interface to retrieve the final status of
 * an asynchronous operation.
 *
 * @param <T> the type of result that this operation produces
 */
public interface IoFuture<T> {
    /**
     * The current status of an asynchronous operation.
     */
    enum Status {
        /**
         * The operation is still in progress.
         */
        WAITING,
        /**
         * The operation has completed successfully.
         */
        DONE,
        /**
         * The operation was cancelled.
         */
        CANCELLED,
        /**
         * The operation did not succeed.
         */
        FAILED,
    }

    /**
     * Cancel an operation.  The actual cancel may be synchronous or asynchronous.
     *
     * @return this {@code IoFuture} instance
     */
    IoFuture<T> cancel();

    /**
     * Get the current status.
     *
     * @return the current status
     */
    Status getStatus();

    /**
     * Wait for the operation to complete.  This method will block until the status changes from {@link Status#WAITING}.
     *
     * @return the new status
     */
    Status await();

    /**
     * Wait for the operation to complete, with a timeout.  This method will block until the status changes from {@link Status#WAITING},
     * or the given time elapses.  If the time elapses before the operation is complete, {@link Status#WAITING} is
     * returned.
     *
     * @param time the amount of time to wait
     * @param timeUnit the time unit
     * @return the new status, or {@link Status#WAITING} if the timeout expired
     */
    Status await(long time, TimeUnit timeUnit);

    /**
     * Wait for the operation to complete.  This method will block until the status changes from {@link Status#WAITING},
     * or the current thread is interrupted.
     *
     * @return the new status
     * @throws InterruptedException if the operation is interrupted
     */
    Status awaitInterruptibly() throws InterruptedException;

    /**
     * Wait for the operation to complete, with a timeout.  This method will block until the status changes from {@link Status#WAITING},
     * the given time elapses, or the current thread is interrupted.  If the time elapses before the operation is complete, {@link Status#WAITING} is
     * returned.
     *
     * @param time the amount of time to wait
     * @param timeUnit the time unit
     * @return the new status, or {@link Status#WAITING} if the timeout expired
     * @throws InterruptedException if the operation is interrupted
     */
    Status awaitInterruptibly(long time, TimeUnit timeUnit) throws InterruptedException;

    /**
     * Get the result of the operation.  If the operation is not complete, blocks until the operation completes.  If
     * the operation fails, or has already failed at the time this method is called, the failure reason is thrown.
     *
     * @return the result of the operation
     * @throws IOException if the operation failed
     * @throws CancellationException if the operation was cancelled
     */
    T get() throws IOException, CancellationException;

    /**
     * Get the result of the operation.  If the operation is not complete, blocks until the operation completes.  If
     * the operation fails, or has already failed at the time this method is called, the failure reason is thrown.  If
     * the current thread is interrupted while waiting, an exception is thrown.
     *
     * @return the result of the operation
     * @throws IOException if the operation failed
     * @throws InterruptedException if the operation is interrupted
     * @throws CancellationException if the operation was cancelled
     */
    T getInterruptibly() throws IOException, InterruptedException, CancellationException;

    /**
     * Get the failure reason.
     *
     * @return the failure reason
     * @throws IllegalStateException if the operation did not fail
     */
    IOException getException() throws IllegalStateException;

    /**
     * Add a notifier to be called when this operation is complete.  If the operation is already complete, the notifier
     * is called immediately, possibly in the caller's thread.  The given attachment is provided to the notifier.
     *
     * @param notifier the notifier to be called
     * @param attachment the attachment to pass in to the notifier
     * @return this instance
     */
    <A> IoFuture<T> addNotifier(Notifier<T, A> notifier, A attachment);

    /**
     * A notifier that handles changes in the status of an {@code IoFuture}.
     */
    interface Notifier<T, A> {
        /**
         * Receive notification of the completion of an outstanding operation.
         *
         * @param ioFuture the future corresponding to this operation
         * @param attachment the attachment
         */
        void notify(IoFuture<T> ioFuture, final A attachment);
    }

    /**
     * A base notifier class that calls the designated handler method on notification.  Use this class to reduce
     * boilerplate for standard {@link org.jboss.xnio.IoFuture.Notifier} implementations.
     *
     * @since 1.1
     */
    abstract class HandlingNotifier<T, A> implements Notifier<T, A> {
        /**
         * {@inheritDoc}
         */
        public void notify(final IoFuture<T> future, A attachment) {
            switch (future.getStatus()) {
                case CANCELLED:
                    handleCancelled(attachment);
                    break;
                case DONE:
                    try {
                        handleDone(future.get(), attachment);
                    } catch (IOException e) {
                        // not possible
                        throw new IllegalStateException();
                    }
                    break;
                case FAILED:
                    handleFailed(future.getException(), attachment);
                    break;
                default:
                    // not possible
                    throw new IllegalStateException();
            }
        }

        /**
         * Handle cancellation.
         *
         * @param attachment the attachment
         */
        public void handleCancelled(final A attachment) {
        }

        /**
         * Handle failure.
         *
         * @param exception the failure reason
         * @param attachment the attachment
         */
        public void handleFailed(final IOException exception, final A attachment) {
        }

        /**
         * Handle completion.
         *
         * @param result the result
         * @param attachment the attachment
         */
        public void handleDone(final T result, final A attachment) {
        }
    }
}
