import { cn } from "cn"

/** A grey block with a light sweeping across it while content loads. */
export function Bone({ className }: { className?: string }) {
  return <div className={cn("skeleton-shimmer rounded-2xl", className)} />
}

/** Placeholder for a lazily loaded page: title, lead and a grid of product tiles. */
export function PageSkeleton() {
  return (
    <div className="mx-auto max-w-[90rem] px-4 pt-10 pb-16 sm:px-6 lg:px-10" aria-busy="true" aria-label="Loading">
      <Bone className="h-4 w-40 rounded-full" />
      <Bone className="mt-6 h-14 w-2/3 max-w-xl" />
      <Bone className="mt-4 h-5 w-full max-w-lg rounded-full" />
      <div className="mt-12 grid grid-cols-2 gap-x-4 gap-y-10 lg:grid-cols-4 lg:gap-x-6">
        {Array.from({ length: 8 }, (_, i) => (
          <div key={i}>
            <Bone className="aspect-square w-full" />
            <Bone className="mt-4 h-4 w-1/2 rounded-full" />
            <Bone className="mt-2 h-4 w-1/3 rounded-full" />
          </div>
        ))}
      </div>
    </div>
  )
}

/** First paint while the store's data loads: header bar, hero and a row of tiles. */
export function StoreSkeleton() {
  return (
    <div aria-busy="true">
      <span className="sr-only" aria-live="polite">Loading the store</span>
      <div className="flex h-16 items-center justify-between border-b px-4 sm:px-6 lg:px-10">
        <Bone className="h-6 w-36 rounded-full" />
        <div className="hidden gap-6 md:flex">{[0, 1, 2, 3].map((i) => <Bone key={i} className="h-4 w-16 rounded-full" />)}</div>
        <div className="flex gap-3">{[0, 1, 2].map((i) => <Bone key={i} className="size-9 rounded-full" />)}</div>
      </div>
      <Bone className="h-[62svh] rounded-none" />
      <div className="mx-auto mt-12 grid max-w-[90rem] grid-cols-2 gap-4 px-4 sm:px-6 lg:grid-cols-4 lg:gap-6 lg:px-10">
        {[0, 1, 2, 3].map((i) => <Bone key={i} className="aspect-square w-full" />)}
      </div>
    </div>
  )
}
